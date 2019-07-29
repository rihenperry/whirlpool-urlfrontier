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
    private static Channel systemChannel;
    private static Channel frontierChannel;

    public static void main( String[] args ) throws Exception
    {

        System.out.println("JAVA_ENV= " + System.getenv("JAVA_ENV"));

        systemChannel = RMQAuth.INSTANCE
                .getInstance()
                .createSystemRMQChannel();

        frontierChannel = RMQAuth.INSTANCE
                .getInstance()
                .createFrontierRMQChannel();
    }
}
