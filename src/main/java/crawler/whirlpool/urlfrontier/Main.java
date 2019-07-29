package crawler.whirlpool.urlfrontier;

import com.rabbitmq.client.Channel;
import crawler.whirlpool.urlfrontier.config.FrontierLogging;
import crawler.whirlpool.urlfrontier.config.RMQAuth;
import crawler.whirlpool.urlfrontier.scheme.BqSelector;
import crawler.whirlpool.urlfrontier.scheme.FqBiasedRamdomizer;
import crawler.whirlpool.urlfrontier.scheme.FqPrioritizer;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class Main
{
    private static final Logger stdlog = FrontierLogging.INSTANCE.getInstance()
            .getLogger("FrontierConsoleLogger");
    private static final Logger filelog= FrontierLogging.INSTANCE.getInstance()
            .getLogger("FrontierFileLogger");
    private static Channel systemChannel;
    private static Channel frontierChannel;

    public static void main( String[] args )
    {

        stdlog.info("using {} settings", System.getenv("JAVA_ENV"));

        try {
            systemChannel = RMQAuth.INSTANCE.getInstance().createSystemRMQChannel();
            frontierChannel = RMQAuth.INSTANCE.getInstance().createFrontierRMQChannel();
            Main exec = new Main();
            exec.go();

        } catch (IOException ioError) {
            filelog.error("io error when getting RMQ channels for main class {}", ioError.getMessage());
        }
    }

    public void go() {
        FqBiasedRamdomizer fqbiased = new FqBiasedRamdomizer();
        BqSelector bqselector = new BqSelector();

        fqbiased.start();
        bqselector.start();
    }
}
