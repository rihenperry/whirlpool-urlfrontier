package crawler.whirlpool.urlfrontier.scheme;

import com.rabbitmq.client.Channel;
import crawler.whirlpool.urlfrontier.config.FrontierLogging;
import org.apache.logging.log4j.Logger;

public class FqBiasedRamdomizer extends Thread {
    private static final Logger stdlog = FrontierLogging.INSTANCE.getInstance()
            .getLogger("FrontierConsoleLogger");
    private static final Logger filelog= FrontierLogging.INSTANCE.getInstance()
            .getLogger("FrontierFileLogger");
    private Channel frontierChannel;

    public FqBiasedRamdomizer() {
        this("fq-biasedRamdomizer-bq-router");
    }

    public FqBiasedRamdomizer(String name) {
        super.setName(name);
    }

    public void run() {
        stdlog.info("running thread {}", this.getName());
    }
}