package com.github.search.config;

import com.github.common.util.U;
import com.google.common.collect.Lists;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SearchDataSourceInit {

    @Value("${search.ip-port:127.0.0.1:9200}")
    private String ipAndPort;
    @Value("${search.username}")
    private String username;
    @Value("${search.password}")
    private String password;

    @Bean
    public RestHighLevelClient search() {
        String[] ipPortArray = ipAndPort.split(",");
        List<HttpHost> hostList = Lists.newArrayListWithCapacity(ipPortArray.length);
        for (String ipAndPort : ipPortArray) {
            hostList.add(HttpHost.create(ipAndPort));
        }

        RestClientBuilder builder = RestClient.builder(hostList.toArray(new HttpHost[0]))
                .setRequestConfigCallback(req ->
                        req.setConnectTimeout(5 * 1000).setSocketTimeout(60 * 1000).setConnectionRequestTimeout(5 * 1000))
                .setMaxRetryTimeoutMillis(60 * 1000);

        if (U.isNotBlank(username) && U.isNotBlank(password)) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }
        return new RestHighLevelClient(builder);
    }
}
