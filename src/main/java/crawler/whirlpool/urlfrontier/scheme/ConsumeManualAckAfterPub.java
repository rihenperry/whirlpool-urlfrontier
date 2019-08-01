package crawler.whirlpool.urlfrontier.scheme;

import java.io.IOException;

public interface ConsumeManualAckAfterPub {
    public abstract void manualAck(long consumeTag) throws IOException;

    public abstract void manualNAck(long consumeTag) throws IOException;
}
