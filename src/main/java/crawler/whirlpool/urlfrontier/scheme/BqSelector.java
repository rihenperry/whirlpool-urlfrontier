package crawler.whirlpool.urlfrontier.scheme;

import com.rabbitmq.client.Channel;
import crawler.whirlpool.urlfrontier.config.FrontierLogging;
import org.apache.logging.log4j.Logger;

public class BqSelector extends Thread {
    private static final Logger stdlog = FrontierLogging.INSTANCE.getInstance()
            .getLogger("FrontierConsoleLogger");
    private static final Logger filelog= FrontierLogging.INSTANCE.getInstance()
            .getLogger("FrontierFileLogger");
    private Channel systemChannel;
    private Channel frontierChannel;

    public BqSelector() {
        this("bq-selector-publisher");
    }

    public BqSelector(String name) {
        super.setName(name);
    }

    public void run() {
        stdlog.info("running thread {}", this.getName());
    }
}
