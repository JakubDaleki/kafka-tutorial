package jdaleki.kafka.twitter;

import jdk.jfr.ContentType;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;

public class ElasticSearchConsumer {

    public static RestHighLevelClient createClient() throws IOException {
        ConfigReader configReader = new ConfigReader();
        Properties properties = configReader.getConfig();
        String hostname = properties.getProperty("elastic_hostname");
        String username = properties.getProperty("elastic_username");
        String password = properties.getProperty("elastic_password");

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));
        RestClientBuilder builder = RestClient.builder(new HttpHost(hostname, 443, "https"))
                .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpAsyncClientBuilder) {
                        return httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                });
        RestHighLevelClient client = new RestHighLevelClient(builder);
        return client;
    }

    public static void main(String[] args) throws IOException {
        String jsonString = "{" +
                "\"user\":\"Bran\"," +
                "\"postDate\":\"2021-03-18\"," +
                "\"message\":\"trying out Elasticsearch\"" +
                "}";

        RestHighLevelClient client = createClient();
        IndexRequest indexRequest = new IndexRequest("twitter")
                .source(jsonString, XContentType.JSON);
        IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);

        System.out.println(indexResponse.getId());

        client.close();

    }
}
