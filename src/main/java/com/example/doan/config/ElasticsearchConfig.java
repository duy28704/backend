package com.example.doan.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

@Configuration
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String elasticsearchUri;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Override
    public ClientConfiguration clientConfiguration() {
        boolean useSsl = elasticsearchUri.startsWith("https://");

        String cleanUri = elasticsearchUri
                .replace("http://", "")
                .replace("https://", "");

        // Remove trailing slash if present
        if (cleanUri.endsWith("/")) {
            cleanUri = cleanUri.substring(0, cleanUri.length() - 1);
        }

        ClientConfiguration.MaybeSecureClientConfigurationBuilder builder =
                ClientConfiguration.builder().connectedTo(cleanUri);

        if (useSsl) {
            builder.usingSsl();
        }

        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            builder.withBasicAuth(username, password);
        }

        return builder.build();
    }
}
