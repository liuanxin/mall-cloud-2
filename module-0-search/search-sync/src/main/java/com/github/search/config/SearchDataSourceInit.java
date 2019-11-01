package com.github.search.config;

//@Configuration
public class SearchDataSourceInit {

//    @Value("${search.ip-port:127.0.0.1:9200}")
//    private String ipAndPort;
//    @Value("${search.username:}")
//    private String username;
//    @Value("${search.password:}")
//    private String password;
//
//    // @see org.springframework.boot.autoconfigure.elasticsearch.rest.RestClientConfigurations.RestHighLevelClientConfiguration
//
//    @Bean
//    public RestHighLevelClient search() {
//        String[] ipPortArray = ipAndPort.split(",");
//        List<HttpHost> hostList = Lists.newArrayListWithCapacity(ipPortArray.length);
//        for (String ipAndPort : ipPortArray) {
//            if (U.isNotBlank(ipAndPort)) {
//                hostList.add(HttpHost.create(ipAndPort.trim()));
//            }
//        }
//
//        RestClientBuilder builder = RestClient.builder(hostList.toArray(new HttpHost[0]))
//                .setRequestConfigCallback(req ->
//                        req.setConnectTimeout(5 * 1000).setSocketTimeout(60 * 1000).setConnectionRequestTimeout(5 * 1000))
//                .setMaxRetryTimeoutMillis(60 * 1000);
//
//        if (U.isNotBlank(username) && U.isNotBlank(password)) {
//            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
//            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
//            builder.setHttpClientConfigCallback(httpClientBuilder ->
//                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
//        }
//        return new RestHighLevelClient(builder);
//    }
}
