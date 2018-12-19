package com.github.search.config;

import com.google.common.collect.Lists;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
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

    @Bean
    public RestHighLevelClient search() {
        String[] ipPortArray = ipAndPort.split(",");
        int length = ipPortArray.length;
        List<HttpHost> hostList = Lists.newArrayListWithCapacity(length);
        for (String ipAndPort : ipPortArray) {
            hostList.add(HttpHost.create(ipAndPort));
        }
        return new RestHighLevelClient(
                RestClient.builder(hostList.toArray(new HttpHost[length]))
                        .setRequestConfigCallback(new RestClientBuilder.RequestConfigCallback() {
                            @Override
                            public RequestConfig.Builder customizeRequestConfig(RequestConfig.Builder request) {
                                return request.setConnectTimeout(5 * 1000)
                                        .setSocketTimeout(60 * 1000)
                                        .setConnectionRequestTimeout(5 * 1000);
                            }
                        })
                        .setMaxRetryTimeoutMillis(60 * 1000)
        );
    }
}
