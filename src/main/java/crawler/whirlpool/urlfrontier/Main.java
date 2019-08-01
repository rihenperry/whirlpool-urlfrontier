package crawler.whirlpool.urlfrontier;

import crawler.whirlpool.urlfrontier.config.FrontierLogging;
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

    public static void main( String[] args )
    {

        stdlog.info("using {} settings", System.getenv("JAVA_ENV"));

        Main exec = new Main();
        new FqPrioritizer();
        exec.go();
    }

    public void go() {
        FqBiasedRamdomizer fqbiased = new FqBiasedRamdomizer();
        BqSelector bqselector = new BqSelector();

        fqbiased.start();
        bqselector.start();
    }
}
