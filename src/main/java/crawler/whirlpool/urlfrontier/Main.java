package crawler.whirlpool.urlfrontier;

import org.apache.logging.log4j.Logger;

public class Main
{
    static  {
        System.setProperty("log4j.configurationFactory", FrontierLogging.class.getName());
    }

    private  static  final Logger stdlog = FrontierLogging.INSTANCE
            .getInstance()
            .getLogger("FrontierConsoleLogger");
    private  static  final Logger filelog= FrontierLogging.INSTANCE
            .getInstance()
            .getLogger("FrontierFileLogger");

    public static void main( String[] args )
    {

        System.out.println("JAVA_ENV= " + System.getenv("JAVA_ENV"));

        stdlog.debug("this is debug level");
        stdlog.info("this is info level");
        filelog.error("this is error level");
        filelog.fatal("this is fatal level");
        stdlog.warn("this is warn level");
    }
}
