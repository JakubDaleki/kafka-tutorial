package jdaleki.kafka.twitter;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TwitterProducer {
    Logger logger = LoggerFactory.getLogger(TwitterProducer.class.getName());
    String consumerKey = "PEQP0MbbRwi66yuULXsJWxbJD";
    String consumerSecret = "BZQ7mIsQB7vDcqVVIseNeMsNkB0QGyoW0SG7PHqJ1CcueYMCHL";
    String token = "1348738935783092226-88zmAqOVn2DaKkkNkhHnTWBvdSxUYv";
    String secret = "lFASOXy7tKoO6wDdZDJ7VCSHs4RAmDc6O6CeDUayHlHmM";
    List<String> terms = Lists.newArrayList("spark");

    public static void main(String[] args) {
        new TwitterProducer().run();
    }

    public void run() {

        BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(100000);

        // create twitter client
        Client client = createTwitterClient(msgQueue);
        // Attempts to establish a connection.
        client.connect();

        // create kafka producer

        KafkaProducer<String, String> producer = createKafkaProducer();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Stopping app");
            client.stop();
            producer.close();
        }));
        // loop to send tweets to kafka

        while (!client.isDone()) {
            String msg = null;
            try {
                msg = msgQueue.poll(5, TimeUnit.SECONDS);
                if (msg != null) {
                    producer.send(new ProducerRecord<String, String>("real_time", null, msg), (RecordMetadata recordMetadata, Exception e) -> {
                        if (e != null) {
                            logger.error("Error occured:", e);
                        }
                        else {
                            logger.info(recordMetadata.topic());
                        }
                    });
                    producer.flush();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                client.stop();
            }
        }
        logger.info("Koniec dzialania");

    }

    public Client createTwitterClient(BlockingQueue<String> msgQueue) {

        /** Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
        // Optional: set up some followings and track terms
        hosebirdEndpoint.trackTerms(terms);

        // These secrets should be read from a config file
        Authentication hosebirdAuth = new OAuth1(consumerKey, consumerSecret, token, secret);

        ClientBuilder builder = new ClientBuilder()
                .name("Hosebird-Client-01")                              // optional: mainly for the logs
                .hosts(hosebirdHosts)
                .authentication(hosebirdAuth)
                .endpoint(hosebirdEndpoint)
                .processor(new StringDelimitedProcessor(msgQueue));

        Client hosebirdClient = builder.build();
        return hosebirdClient;
    }

    private KafkaProducer<String, String> createKafkaProducer() {
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // safe producer
        properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        properties.setProperty(ProducerConfig.ACKS_CONFIG, "all");
        properties.setProperty(ProducerConfig.RETRIES_CONFIG, Integer.toString(Integer.MAX_VALUE));
        properties.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5");

        // high throughput
        properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "20");
        properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32*1024));


        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);

        return producer;
    }
}
