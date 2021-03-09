package jdaleki.kafka.tutorial;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class ConsumerRunnable implements Runnable {

    private CountDownLatch latch;
    private KafkaConsumer<String, String> consumer;
    private Logger logger = LoggerFactory.getLogger(ConsumerRunnable.class.getName());

    public ConsumerRunnable(CountDownLatch latch, String bootstrapServer, String groupId, String topic) {
        this.latch = latch;

        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        consumer = new KafkaConsumer<String, String>(properties);

        // subscribe
        consumer.subscribe(Collections.singleton(topic));
    }

    @Override
    public void run() {
        // poll
        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record : records) {
                    logger.info("Key: " + record.key(), "Value: " + record.value());
                    logger.info("Partition: " + record.partition(), "Offset: " + record.offset());
                }
            }
        } catch (WakeupException e) {
            logger.info("Received shutdown signal!");
        } finally {
            consumer.close();
            latch.countDown();
        }
    }

    public void shutdown() {
        consumer.wakeup();
    }
}