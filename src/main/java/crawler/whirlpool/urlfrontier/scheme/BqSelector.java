package crawler.whirlpool.urlfrontier.scheme;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import crawler.whirlpool.urlfrontier.config.FrontierLogging;
import crawler.whirlpool.urlfrontier.config.RMQAuth;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class BqSelector extends Thread implements ConsumeManualAckAfterPub{
    private static final Logger stdlog = FrontierLogging.INSTANCE.getInstance()
            .getLogger("FrontierConsoleLogger");
    private static final Logger filelog= FrontierLogging.INSTANCE.getInstance()
            .getLogger("FrontierFileLogger");
    private Channel bqSelectorSystemChannel;
    private Channel bqSelectorfrontierChannel;

    long bqSelectorPubAckCount = 0;
    Map<Long, Long> bqSelectorConsumeAckSet = Collections.synchronizedSortedMap(new TreeMap<Long, Long>());

    public BqSelector() {
        this("bq-selector-publisher");
    }

    public BqSelector(String name) {
        super.setName(name);

        try {
            this.bqSelectorSystemChannel = RMQAuth.INSTANCE.getInstance().createSystemRMQChannel();
            this.bqSelectorfrontierChannel = RMQAuth.INSTANCE.getInstance().createFrontierRMQChannel();

            this.bqSelectorfrontierChannel.confirmSelect();
            this.bqSelectorfrontierChannel.addConfirmListener(new PubSubAckListener(this) {
                @Override
                public Map<Long, Long> getConsumeAckSet() {
                    return bqSelectorConsumeAckSet;
                }

                @Override
                public String getAckHandle() {
                    return "bqSelector";
                }
            });
        } catch (IOException ioerror) {
            filelog.error("io error when getting system/frontier vhost RMQ channels", ioerror.getMessage());
        }
    }

    public void run() {
        stdlog.info("running thread {} handling msg flow from bq.router out to urlfrontier publisher",
                this.getName());

        try {
            this.consumeBqueues();
        } catch (InterruptedException interrupt) {
            stdlog.warn("running thread interrupted {}", interrupt.getMessage());
        } catch (IOException ioerror) {
            filelog.error("Caught io error when comsuming pull style message from back.queues {}",
                    ioerror.getMessage());
            ioerror.printStackTrace();
        }
    }

    private void consumeBqueues() throws InterruptedException, IOException {
        //This handles politeness logic. Here, access the frontier heap to pull an item from
        //the right queue to be crawled. Make the heap volatile and add code to serialize it.
        boolean autoAck = false;
        //this is just temp logic to test msg flow across whirlpool subsystems
        while (true) {
            for (short level = 1; level <= 3; level++) {
                for (short qNo = 1; qNo <= level; qNo++) {
                    GetResponse response = this.bqSelectorfrontierChannel
                            .basicGet("back.q.".concat(Short.toString(qNo)), autoAck);
                    if (response == null) {
                        stdlog.warn("no payload found on back.q.{}", qNo);
                    } else {
                        AMQP.BasicProperties props = response.getProps();
                        byte[] body = response.getBody();
                        long delTag = response.getEnvelope().getDeliveryTag();

                        //pass the extracted body and delivery tag to publisher
                        JSONObject obj = new JSONObject(
                                IOUtils.toString(new ByteArrayInputStream(body),
                                StandardCharsets.UTF_8));

                        stdlog.info("payload found on back.q.{}, level {}", qNo, level);
                        this.publishToFetcherQ(delTag, obj);
                    }
                    stdlog.info("sleeping before fetching next back.q. for {} secs",1);

                    Thread.sleep(1000);
                } //end of inner for loop

            } //end of outer for loop

            Thread.sleep(2000);
        } //end of while loop
    }

    @Override
    public void manualAck(long consumeTag) throws IOException {
        this.bqSelectorfrontierChannel.basicAck(consumeTag, false);
        stdlog.info("bqSelector manual ack and consumer delivery tag={}", consumeTag);
    }

    @Override
    public void manualNAck(long consumeTag) throws IOException {
        this.bqSelectorfrontierChannel.basicNack(consumeTag, true, false);
        filelog.error("bqSelector manualNack consumer tag not ack {}", consumeTag);
    }

    private void publishToFetcherQ(long delTag, JSONObject payload) throws IOException{
        // just publish to fetcherQ
        stdlog.info("got payload {} to publish to fetcher.q", payload.toString());

        // this temp hash map of domain <-> q_no

        String exName = "urlfrontier.ex.fetcher";
        String routeKey = "urlfrontier_p.to.fetcher_c";

        stdlog.info("routing url {} to fetcher.q", payload.get("url"));

        // repackage msg for bq.queue consumption
        byte[] msgInBytes = payload.toString().getBytes(StandardCharsets.UTF_8);

        Map<String, Object> headers = new HashMap<>();
        headers.put("from", "urlfrontier");

        this.bqSelectorSystemChannel.basicPublish(exName, routeKey,
                new AMQP.BasicProperties.Builder()
                        .headers(headers)
                        .contentType("application/json")
                        .deliveryMode(2)
                        .build(), msgInBytes);
        bqSelectorConsumeAckSet.put(++bqSelectorPubAckCount, delTag);
    }
}
