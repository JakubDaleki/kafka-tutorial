package jdaleki.kafka.twitter;

import java.io.*;
import java.util.Properties;

public class ConfigReader {
    private String fileName = "./kafka-tutorial/kafka-twitter-producer/resources/config.properties";
    private Properties prop;

    public ConfigReader() throws IOException {
        prop = new Properties();
        InputStream inputStream = null;

        inputStream = new FileInputStream(fileName);
        prop.load(inputStream);

    }

    public Properties getConfig() {
        return prop;
    }
}
