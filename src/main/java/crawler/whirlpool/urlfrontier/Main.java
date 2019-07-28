package crawler.whirlpool.urlfrontier;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import crawler.whirlpool.urlfrontier.Config.FrontierLogging;
import crawler.whirlpool.urlfrontier.Config.RMQAuth;
import org.apache.logging.log4j.Logger;

public class Main
{
    private static final Logger stdlog = FrontierLogging.INSTANCE
            .getInstance()
            .getLogger("FrontierConsoleLogger");
    private static final Logger filelog= FrontierLogging.INSTANCE
            .getInstance()
            .getLogger("FrontierFileLogger");


    public static void main( String[] args )
    {

        System.out.println("JAVA_ENV= " + System.getenv("JAVA_ENV"));

        ConnectionFactory factory = new ConnectionFactory();
        stdlog.debug("this is debug level");
        stdlog.info("this is info level");
        filelog.error("this is error level");
        filelog.fatal("this is fatal level");
        stdlog.warn("this is warn level");

        Channel systemChannel = RMQAuth.INSTANCE
                .getInstance()
                .getSystemRMQChannel();

        Channel frontierChannel = RMQAuth.INSTANCE
                .getInstance()
                .getFrontierRMQChannel();
    }
}
