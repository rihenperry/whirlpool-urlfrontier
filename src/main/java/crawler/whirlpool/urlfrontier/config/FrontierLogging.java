package crawler.whirlpool.urlfrontier.config;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

public enum FrontierLogging {

    INSTANCE();

    private String configName;
    private LoggerContext ctx;

    private FrontierLogging() {
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        this.configName = "URLFrontierLogging";
        builder.setConfigurationName(this.configName);
        // include a common log formatter for console and file log handlers
        LayoutComponentBuilder formatter = builder.newLayout("PatternLayout");
        formatter.addAttribute("pattern",
                "%d{yy-MM-dd HH:mm:ss:SSS} [%t] %highlight{%level}{FATAL=bg_red, ERROR=red, WARN=yellow, INFO=green, DEBUG=blue} %F:%L - %m%n");


        // setup triggering policy for rotating file handler
        ComponentBuilder triggeringPolicy = builder.newComponent("Policies")
                .addComponent(builder.newComponent("CronTriggeringPolicy")
                        .addAttribute("schedule", "0 0 0 * * ?"))
                .addComponent(builder.newComponent("SizeBasedTriggeringPolicy").addAttribute("size", "50M"));

        // log to console handler
        AppenderComponentBuilder console = builder.newAppender("stdout", "CONSOLE")
                .addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT)
                .add(formatter);
        builder.add(console);

        // log to rotating file handler
        AppenderComponentBuilder rotatingFile = builder.newAppender("rotating", "RollingFile")
                .addAttribute("fileName", "logs/rotating.log")
                .addAttribute("filePattern", "logs/rotating-%d{MM-dd-yy}.log.gz")
                .add(formatter)
                .addComponent(triggeringPolicy);
        builder.add(rotatingFile);

        // root logging configuration must be declared
        builder.add(builder.newRootLogger(Level.ERROR)
                .add(builder.newAppenderRef("rotating")));

        //setup loggers for frontier
        LoggerComponentBuilder frontierConsoleLogger = builder.newLogger("FrontierConsoleLogger", Level.DEBUG)
                .add(builder.newAppenderRef("stdout"))
                .addAttribute("additivity", false);
        builder.add(frontierConsoleLogger);

        LoggerComponentBuilder frontierFileLogger = builder.newLogger("FrontierFileLogger", Level.ERROR)
                .add(builder.newAppenderRef("rotating"))
                .addAttribute("additivity", false);
        builder.add(frontierFileLogger);

        this.ctx = Configurator.initialize(builder.build());
    }

    public FrontierLogging getInstance() {
        return INSTANCE;
    }


    public Logger getLogger(Class<?> clazz) {
        return LogManager.getLogger(clazz);
    }


    public Logger getLogger(String logName) {
        return this.ctx.getLogger(logName);
    }

}
