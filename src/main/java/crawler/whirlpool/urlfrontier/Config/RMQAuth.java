package crawler.whirlpool.urlfrontier.Config;


import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
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

    private Connection systemRMQConnection;
    private Connection frontierRMQConnection;
    private HashMap<String, String> sysVhostURI;
    private HashMap<String, String> frontierVhostURI;

    private  RMQAuth() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Properties props = new Properties();
        try {
            props.load(loader.getResourceAsStream("rmqconfig.properties"));
            stdlog.info("reading rabbitmq configuration...");

            //build connection strings and connect to respective vhost
            HashMap<String, String> baseURI = new HashMap<>();
            baseURI.put("host", props.getProperty("hostname"));
            baseURI.put("port", props.getProperty("port"));

            this.sysVhostURI = new HashMap<String, String>();
            this.sysVhostURI.putAll(baseURI);

            this.sysVhostURI.put("user", props.getProperty("system_user"));
            this.sysVhostURI.put("password", props.getProperty("system_pwd"));
            this.sysVhostURI.put("vhost", props.getProperty("system_vhost"));

            stdlog.info("sys uri {}", this.rmqURIBuilder(this.sysVhostURI));

            this.systemRMQConnection = this.setupRMQVhost(this.rmqURIBuilder(this.sysVhostURI));
            stdlog.info("authenticated to rmq {} vhost as user {} ",
                    this.sysVhostURI.get("vhost"),
                    this.sysVhostURI.get("user"));

            this.frontierVhostURI = new HashMap<String, String>();
            this.frontierVhostURI.putAll(baseURI);
            this.frontierVhostURI.put("user", props.getProperty("frontier_user"));
            this.frontierVhostURI.put("password", props.getProperty("frontier_pwd"));
            this.frontierVhostURI.put("vhost", props.getProperty("frontier_vhost"));

            stdlog.info("frontier uri {}", this.rmqURIBuilder(this.frontierVhostURI));
            this.frontierRMQConnection = this.setupRMQVhost(this.rmqURIBuilder(this.frontierVhostURI));
            stdlog.info("authenticated to rmq {} vhost as user {} ",
                    this.frontierVhostURI.get("vhost"),
                    this.frontierVhostURI.get("user"));
        } catch (Exception ex) {
            filelog.error("rmqconfig.properties not found {}", ex.getStackTrace());
        }
    }

    private String rmqURIBuilder(HashMap<String, String> params) {
        StringBuilder uriObject;

        // build connection strings
        uriObject = new StringBuilder("amqp://");
        uriObject
                .append(params.get("user"))
                .append(":")
                .append(params.get("password"))
                .append("@")
                .append(params.get("host"))
                .append(":")
                .append(params.get("port"))
                .append("/")
                .append(params.get("vhost"));

        return uriObject.toString();
    }

    private Connection setupRMQVhost(String uri) {
        Connection conn = null;
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUri(uri);
            conn = factory.newConnection();
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
        } finally {
            return conn;
        }
    }

    public RMQAuth getInstance() {
        return INSTANCE;
    }

    public Channel createSystemRMQChannel() throws IOException {
        Channel sysChannel = this.systemRMQConnection.createChannel();
        stdlog.info("created channel for rmq user {} ",
                    this.sysVhostURI.get("user"));
        return sysChannel;
    }

    public Channel createFrontierRMQChannel() throws  IOException {
        Channel frontierChannel = this.frontierRMQConnection.createChannel();
        stdlog.info("created channel for user {} ",
                    this.frontierVhostURI.get("user"));
        return  frontierChannel;
    }
}