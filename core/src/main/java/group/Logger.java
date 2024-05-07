package group;

import org.apache.log4j.PropertyConfigurator;

public class Logger {
    public static void configure() {
        PropertyConfigurator.configure(Logger.class.getResource("/log4J.properties"));
    }
}
