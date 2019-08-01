package crawler.whirlpool.urlfrontier.scheme;

import com.rabbitmq.client.*;
import crawler.whirlpool.urlfrontier.config.FrontierLogging;
import crawler.whirlpool.urlfrontier.config.RMQAuth;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class FqPrioritizer implements ConsumeManualAckAfterPub {

    private static final Logger stdlog = FrontierLogging.INSTANCE.getInstance()
            .getLogger("FrontierConsoleLogger");
    private static final Logger filelog= FrontierLogging.INSTANCE.getInstance()
            .getLogger("FrontierFileLogger");

    private Channel systemChannel;
    private Channel frontierChannel;

    long pubAckcount = 0;
    volatile Map<Long, Long> consumeAckSet = Collections.synchronizedSortedMap(new TreeMap<Long, Long>());

    public FqPrioritizer() {
        this.initChannels();
        this.consumeURLFrontierQ();
    }

    private void initChannels () {
        try {
            this.systemChannel = RMQAuth.INSTANCE.getInstance().createSystemRMQChannel();
            this.frontierChannel = RMQAuth.INSTANCE.getInstance().createFrontierRMQChannel();

            this.systemChannel.basicQos(1);
            this.frontierChannel.confirmSelect();
            this.frontierChannel.addConfirmListener(new RamdomPubSubConfirmListener(this));

            // declare dynamic front queues which control crawler priority. Note that queues are idempotent
            // queue config name, durable=true, exclusive=false, auto-delete=false, args={x-queue-mode: memory}
            for (int i=1; i<=5; i++) {
                String qName = "front.q.".concat(Integer.toString(i));
                String routeKey = "route.prioritizer.to.randomizer.fq".concat(Integer.toString(i));
                this.frontierChannel.queueDeclare(qName, true, false, false, null);
                this.frontierChannel.queueBind(qName,"fq_prioritizer.ex", routeKey);
            }
        } catch (IOException ioError) {
            filelog.error("io error when getting RMQ channels for main class {}", ioError.getMessage());
        }
    }

    private void consumeURLFrontierQ() {
        boolean autoAck = false;
        try {

            this.systemChannel.basicConsume("urlfrontier.q", autoAck, "urlfrontier_consumer_tag",
                new DefaultConsumer(this.systemChannel) {
                        @Override
                        public void handleDelivery(String consumerTag,
                                               Envelope envelope,
                                               AMQP.BasicProperties properties,
                                               byte[] body) throws IOException
                        {
                            // (process the message components here ...)
                            String routingKey = envelope.getRoutingKey();
                            String contentType = properties.getContentType();

                            stdlog.info("consumer messaged properties route_key={}, content_type={}",
                                    routingKey,
                                    contentType);

                            // 1. incoming msg body should be json, convert bytes to JSON object/java Map
                            // 2. JSON is objects in array, iterate through them
                            // 3. each object contains a {int(priority) -> [{url: <str>, type <str>}]}
                            InputStream stream = new ByteArrayInputStream(body);
                            String content = IOUtils.toString(stream, StandardCharsets.UTF_8);
                            JSONObject data = new JSONObject(content);

                            stdlog.info("msg body from urlfrontier.q {}", data.toString());

                            // publish to front queue via some exchange and acknowledge consumer
                            // and publisher message manually. Also pass the data payload
                            //systemChannel.basicAck(envelope.getDeliveryTag(), false);
                            publishToRamdomizer(envelope.getDeliveryTag(), data);
                    }
                });
            } catch (IOException ioerror) {
                filelog.error("Caught io error when comsuming message from urlfrontier.q {}", ioerror.getMessage());
                ioerror.printStackTrace();
            }
    }

    @Override
    public void manualAck(long tag) throws IOException {
        this.systemChannel.basicAck(tag, false);
        stdlog.info("manual ack and consumer delivery tag={}", tag);
        //stdlog.info("consumer manual ack tag={}, removed ? {}", tag, status);
    }

    @Override
    public void manualNAck(long tag) throws IOException {
        stdlog.warn("consumer tag not ack {}", tag);
    }

    private void publishToRamdomizer(long delTag, JSONObject payload) throws IOException {
        // loop through the actual payload and push to the right queue given its priority
        //Iterator<Long> elm = ackSet.iterator();
        //while (elm.hasNext())
          //  stdlog.info("puback set inside pubtorandom {}", elm.next());

        String exName = "fq_prioritizer.ex";
        Iterator<String> keys = payload.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            String routeKey = "route.prioritizer.to.randomizer.fq".concat(key);

            if (payload.get(key) instanceof JSONObject) {
                byte[] msgInBytes = payload.get(key).toString().getBytes(StandardCharsets.UTF_8);

                ++pubAckcount;
                this.frontierChannel.basicPublish(exName, routeKey,
                        new AMQP.BasicProperties.Builder()
                                .contentType("application/json")
                                .deliveryMode(2)
                                .build(), msgInBytes);

            } else if (payload.get(key) instanceof JSONArray) {
                JSONArray urlList = (JSONArray)payload.get(key);

                for (int i = 0; i < urlList.length(); i++) {
                    JSONObject  urlObj = urlList.getJSONObject(i);

                    byte[] msgInBytes = urlObj.toString().getBytes(StandardCharsets.UTF_8);

                    ++pubAckcount;
                    this.frontierChannel.basicPublish(exName, routeKey,
                            new AMQP.BasicProperties.Builder()
                                    .contentType("application/json")
                                    .deliveryMode(2)
                                    .build(), msgInBytes);
                }
            } else {
                filelog.warn("unable to handle parse object inside json {}", payload.get(key));
            }
        } // end of while loop

        // map pub confirms to consume ack tag
        if (pubAckcount != 0) {
            consumeAckSet.put(pubAckcount, delTag);
        }


        // this is the decode of above message at the consumer end
        //JSONObject obj = new JSONObject(IOUtils.toString(new ByteArrayInputStream(b), StandardCharsets.UTF_8));
    }

    class RamdomPubSubConfirmListener implements ConfirmListener {
        private ConsumeManualAckAfterPub consumer;

        RamdomPubSubConfirmListener(ConsumeManualAckAfterPub consumer) {
            this.consumer = consumer;
        }
        @Override
        public void handleAck(long seqNo, boolean b) throws IOException {
            //long manualAck = ackSet.contains(seqNo) == true? seqNo: -1;
            long consumeTag = consumeAckSet.containsKey(seqNo) == true? consumeAckSet.get(seqNo): -1;

            if (consumeTag > 0) {
                stdlog.debug("handle ack: found consumeAckset element {}, pubconfirm seq No {}, boolean {}", consumeTag, seqNo, b);
                consumeAckSet.remove(seqNo);
                this.consumer.manualAck(consumeTag);
            } else {
                stdlog.warn("handle ack: not found consumeAckset element {}, pubconfirm seq No {}, boolean {}", consumeTag, seqNo, b);
            }

//            Iterator<Long> elm = ackSet.iterator();
//            while (elm.hasNext())
//                stdlog.info("puback set inside pub listener handle ack {}, seqNo {}, bool {}",
//                        elm.next(),
//                        seqNo,
//                        b);

//            if (b) {
//                stdlog.info("multiple pub confirm handleAck long seq no={}, boolean b={}", seqNo, b);

//                for (long i = ackSet.first(); i <= seqNo; ++i) {
//                    ackSet.remove(i);
//                }
//            } else {
//                stdlog.info("single pub confirm handleAck long seq no={}, boolean b={} ", seqNo, b);
//                ackSet.remove(seqNo);
//            }
        }

        @Override
        public void handleNack(long l, boolean b) {
            if (b) {
                stdlog.warn("multiple pub confirm No Ack handler long seq no={}, boolean b={}",l, b);
            } else {
                stdlog.warn("single No Ack handler long seq no={}, boolean b={} ",l, b);
            }
        }
    }
}