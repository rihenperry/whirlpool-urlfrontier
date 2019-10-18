package crawler.whirlpool.urlfrontier.scheme;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.GetResponse;
import crawler.whirlpool.urlfrontier.config.FrontierLogging;
import crawler.whirlpool.urlfrontier.config.RMQAuth;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class FqBiasedRamdomizer extends Thread implements ConsumeManualAckAfterPub {
    public static volatile HashMap<String, Integer> BQ_LOOKUP;
    private Map<Long, Long> bqRouterManualAckSet = Collections.synchronizedSortedMap(new TreeMap<Long, Long>());

    static {
        // make this more sophisticated, also used by other thread in urlfrontier
        BQ_LOOKUP = new HashMap<>();
        BQ_LOOKUP.put("dice.com", 1);
        BQ_LOOKUP.put("simplyhired.com", 2);
        BQ_LOOKUP.put("monster.com", 3);
        BQ_LOOKUP.put("job-openings.monster.com", 3);
    }

    private static final Logger stdlog = FrontierLogging.INSTANCE.getInstance()
            .getLogger("FrontierConsoleLogger");
    private static final Logger filelog= FrontierLogging.INSTANCE.getInstance()
            .getLogger("FrontierFileLogger");
    private Channel bqRouterFrontierChannel;
    private long bqRouterPubAckCount = 0;

    public FqBiasedRamdomizer() {
        this("fq-biasedRamdomizer-bq-router");
    }

    public FqBiasedRamdomizer(String name) {
        super.setName(name);

        try {
            this.bqRouterFrontierChannel = RMQAuth.INSTANCE.getInstance().createFrontierRMQChannel();
            this.bqRouterFrontierChannel.confirmSelect();
            this.bqRouterFrontierChannel.addConfirmListener(new PubSubAckListener(this) {
                @Override
                public String getAckHandle() {
                    return "bqRouter";
                }

                @Override
                public Map<Long, Long> getConsumeAckSet() {
                    return bqRouterManualAckSet;
                }
            });

            // 2. declare dynamic back queues which control crawler politeness.
            for (int i=1; i<=3; i++) {
                String qName = "back.q.".concat(Integer.toString(i));
                String routeKey = "route.bqrouter.to.selector.bq".concat(Integer.toString(i));
                this.bqRouterFrontierChannel.queueDeclare(qName, true, false, false, null);
                this.bqRouterFrontierChannel.queueBind(qName,"bq_router.ex", routeKey);
            }

            // 3. init and maintain a mapping of host <-> queues. as can see in static initializer

            stdlog.info("back queues init");
        } catch (IOException ioerror) {
            filelog.error("io error when getting frontier vhost RMQ channel ", ioerror.getMessage());
        }
    }

    public void run() {
        stdlog.info("running thread {}, handling msg flow from fq.randomizer to" +
                "bq.router", this.getName());
        try {
            this.consumeFrontQueues();
        } catch (InterruptedException interrupt) {
            stdlog.warn("running thread interrupted {}", interrupt.getMessage());
        } catch (IOException ioerror) {
            filelog.error("Caught io error when comsuming pull style message from fron.queues {}",
                    ioerror.getMessage());
            ioerror.printStackTrace();
        } catch (URISyntaxException uriError) {
            filelog.error("Caught io error when comsuming pull style message from fron.queues {}",
                    uriError.getMessage());
            uriError.printStackTrace();
        }
    }

    private void  consumeFrontQueues() throws InterruptedException, IOException, URISyntaxException {
        // this consumer should use appropriate design pattern to easily switch
        // between different consume techniques. I am using a consuming high priority
        // queues more often than low priority ones.
        boolean autoAck = false;

        while (true) {
            for (short level = 1; level <= 5; level++) {
                for (short qNo = 1; qNo <= level; qNo++) {
                    GetResponse response = this.bqRouterFrontierChannel
                            .basicGet("front.q.".concat(Short.toString(qNo)), autoAck);
                    if (response == null) {
                        stdlog.warn("no payload found on front.q.{}", qNo);
                    } else {
                        AMQP.BasicProperties props = response.getProps();
                        byte[] body = response.getBody();
                        long delTag = response.getEnvelope().getDeliveryTag();

                        //pass the extracted body and delivery tag to publisher
                        JSONObject obj = new JSONObject(
                                IOUtils.toString(new ByteArrayInputStream(body),
                                StandardCharsets.UTF_8));

                        stdlog.info("payload found on front.q.{}, level {}", qNo, level);
                        this.publishToBqueues(delTag, obj);
                    }
                    stdlog.info("sleeping before fetching next front.q. for {} secs",1);

                    Thread.sleep(1000);
                } //end of inner for loop

            } //end of outer for loop

            Thread.sleep(2000);
        } //end of while loop
    }

    @Override
    public void manualAck(long tag) throws IOException {
        this.bqRouterFrontierChannel.basicAck(tag, false);
        stdlog.info("bqBiasedRamdomizer manual ack and consumer delivery tag={}", tag);
    }

    @Override
    public void manualNAck(long tag) throws IOException {
        this.bqRouterFrontierChannel.basicNack(tag, true, false);
        filelog.error("bqBiasedRandomizer manualNack consumer tag not ack {}", tag);
    }


    private void publishToBqueues(long delTag, JSONObject payload) throws IOException, URISyntaxException {
        // 1. get the payload and extract and resolve the domain name to q.no from url key in order
        // to push the message to the right back queue.
        // 2. at the same time, insert an entry of this url onto min-priority heap
        // for bq selector thread to pull this url at the given time
        stdlog.info("got payload {} to publish to b.queues", payload.toString());

        // this temp hash map of domain <-> q_no

        String exName = "bq_router.ex";

        URI uri = new URI(payload.getString("url"));
        String domain = uri.getHost();
        String host = domain.startsWith("www.") ? domain.substring(4) : domain;
        String routeKey = "route.bqrouter.to.selector.bq".concat(FqBiasedRamdomizer.BQ_LOOKUP.get(host).toString());

        stdlog.info("routing url {} to back.q.{}", payload.get("url"), FqBiasedRamdomizer.BQ_LOOKUP.get(host));

        // repackage msg for bq.queue consumption
        byte[] msgInBytes = payload.toString().getBytes(StandardCharsets.UTF_8);

        this.bqRouterFrontierChannel.basicPublish(exName, routeKey,
                new AMQP.BasicProperties.Builder()
                        .contentType("application/json")
                        .deliveryMode(2)
                        .build(), msgInBytes);
        bqRouterManualAckSet.put(++this.bqRouterPubAckCount, delTag);
    }
}