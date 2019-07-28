package crawler.whirlpool.urlfrontier.Config;


import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

public enum RMQAuth {

    INSTANCE();

    private final Logger stdlog = FrontierLogging.INSTANCE
            .getInstance()
            .getLogger("FrontierConsoleLogger");
    private final Logger filelog= FrontierLogging.INSTANCE
            .getInstance()
            .getLogger("FrontierFileLogger");
    private Channel systemRMQChannel;
    private Channel frontierRMQChannel;

    private  RMQAuth() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Properties props = new Properties();
        try {
            props.load(loader.getResourceAsStream("rmqconfig.properties"));
            stdlog.info("reading rabbitmq configuration...");

            //build connection strings and connect to respective vhost
            this.setRMQSystemVhost(this.buildRMQSystemURI(props), props);
            this.setRMQFrontierVhost(this.buildRMQFrontierURI(props), props);
        } catch (Exception ex) {
            filelog.error("rmqconfig.properties not found {}", ex.getStackTrace());
        }
    }

    private String buildRMQSystemURI(Properties props) {
        StringBuilder systemVhostURI;

        // build connection strings
        systemVhostURI = new StringBuilder("amqp://");
        systemVhostURI
                .append(props.getProperty("system_user"))
                .append(":")
                .append(props.getProperty("system_pwd"))
                .append("@")
                .append(props.getProperty("hostname"))
                .append(":")
                .append(props.getProperty("port"))
                .append("/")
                .append(props.getProperty("system_vhost"));

        return systemVhostURI.toString();
    }

    private String buildRMQFrontierURI(Properties props) {
        StringBuilder frontierVhostURI;

        // build connection strings
        frontierVhostURI = new StringBuilder("amqp://");
        frontierVhostURI
                .append(props.getProperty("frontier_user"))
                .append(":")
                .append(props.getProperty("frontier_pwd"))
                .append("@")
                .append(props.getProperty("hostname"))
                .append(":")
                .append(props.getProperty("port"))
                .append("/")
                .append(props.getProperty("frontier_vhost"));

        return frontierVhostURI.toString();
    }

    private void setRMQSystemVhost(String uri, Properties props) {
        stdlog.info("rmq system vhost = {}", uri);
        Connection conn;

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUri(uri);
            conn = factory.newConnection();
            stdlog.info("authenticated to rmq system vhost as user {} ",
                    props.getProperty("system_user"));
            this.systemRMQChannel = conn.createChannel();
            stdlog.info("created channel for user {} ",
                    props.getProperty("system_user"));
        } catch (URISyntaxException uriError) {
            filelog.error("URI Syntax {}", uriError.getStackTrace());
        } catch (KeyManagementException keyError) {
            filelog.error("Key malformed {}", keyError.getStackTrace());
        } catch (NoSuchAlgorithmException noAlgo) {
            filelog.error("{}", noAlgo.getStackTrace());
        } catch (IOException ioError) {
            filelog.error("IO fault {}", ioError.getStackTrace());
        } catch (TimeoutException timeoutError) {
            filelog.error("Connection timeout {}", timeoutError.getStackTrace());
        }

    }

    private void setRMQFrontierVhost(String uri, Properties props) {
        stdlog.info("rmq frontier vhost = {}", uri);

        Connection conn;

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUri(uri);
            conn = factory.newConnection();
            stdlog.info("authenticated to rmq frontier vhost as user {} ",
                    props.getProperty("frontier_user"));
            this.frontierRMQChannel = conn.createChannel();
            stdlog.info("created channel for user {} ",
                    props.getProperty("frontier_user"));
        } catch (URISyntaxException uriError) {
            filelog.error("URI Syntax {}", uriError.getStackTrace());
        } catch (KeyManagementException keyError) {
            filelog.error("Key malformed {}", keyError.getStackTrace());
        } catch (NoSuchAlgorithmException noAlgo) {
            filelog.error("{}", noAlgo.getStackTrace());
        } catch (IOException ioError) {
            filelog.error("IO fault {}", ioError.getStackTrace());
        } catch (TimeoutException timeoutError) {
            filelog.error("Connection timeout {}", timeoutError.getStackTrace());
        }
    }

    public RMQAuth getInstance() {
        return INSTANCE;
    }

    public Channel getSystemRMQChannel() {
        return this.systemRMQChannel;
    }

    public Channel getFrontierRMQChannel() {
        return this.frontierRMQChannel;
    }
}
