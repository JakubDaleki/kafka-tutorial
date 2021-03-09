package jdaleki.kafka.tutorial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public class ConsumerDemoWithThread {

    public static void main(String[] args) {
        String bootstrapServer = "127.0.0.1:9092";
        String groupId = "my-fourth-application";
        String topic = "first_topic";
        Logger logger = LoggerFactory.getLogger(ConsumerDemoWithThread.class.getName());
        CountDownLatch latch = new CountDownLatch(1);
        ConsumerRunnable consumerRunnable = new ConsumerRunnable(latch, bootstrapServer, groupId, topic);

        Thread consumerThread = new Thread(consumerRunnable);
        consumerThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("KONIEC PRACY SHUTDOWN!");
            consumerRunnable.shutdown();
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
