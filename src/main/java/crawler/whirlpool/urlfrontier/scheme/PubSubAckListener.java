package crawler.whirlpool.urlfrontier.scheme;

import com.rabbitmq.client.ConfirmListener;
import crawler.whirlpool.urlfrontier.config.FrontierLogging;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map;

abstract class PubSubAckListener implements ConfirmListener {
    private ConsumeManualAckAfterPub consumer;

    private static final Logger stdlog = FrontierLogging.INSTANCE.getInstance()
            .getLogger("FrontierConsoleLogger");
    private static final Logger filelog= FrontierLogging.INSTANCE.getInstance()
            .getLogger("FrontierFileLogger");

    PubSubAckListener(ConsumeManualAckAfterPub consumer) {
        this.consumer = consumer;
    }

    public abstract Map<Long, Long> getConsumeAckSet();

    public abstract String getAckHandle();

    @Override
    public void handleAck(long seqNo, boolean b) throws IOException {
        long consumeTag = getConsumeAckSet().containsKey(seqNo) == true? getConsumeAckSet().get(seqNo): -1;

        if (consumeTag > 0) {
            stdlog.info("{} ack: found pub confirmed maker for consumer ACK. ConsumeAckset element {}," +
                    " pubconfirm seq No {}, boolean {}", getAckHandle(), consumeTag, seqNo, b);
            getConsumeAckSet().remove(seqNo);
            this.consumer.manualAck(consumeTag);
        } else {
            stdlog.debug("{} ack: waiting for consumeAckset element {}, pubconfirm seq No {}," +
                    " boolean {}", getAckHandle(), consumeTag, seqNo, b);
        }
    }

    @Override
    public void handleNack(long l, boolean b) throws IOException {
        long consumeTag = getConsumeAckSet().containsKey(l) == true? getConsumeAckSet().get(l): -1;

        if (consumeTag > 0) {
            filelog.error("{} Nack: missing pub confirmed marker for consumer ACK. consumeAckset element {}," +
                            " pubconfirm seq No {}, boolean {}", getAckHandle(), consumeTag, l, b);
            this.consumer.manualNAck(consumeTag);
        } else {
            stdlog.warn("{} Nack: waiting for consumeAckset element {}, pubconfirm seq No {}, boolean {}",
                    getAckHandle(), consumeTag, l, b);
        }
    }
}
