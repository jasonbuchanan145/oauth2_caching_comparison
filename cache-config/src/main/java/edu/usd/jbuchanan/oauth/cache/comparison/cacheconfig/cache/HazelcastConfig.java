package edu.usd.jbuchanan.oauth.cache.comparison.cacheconfig.cache;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastConfig {

    @Value("${hazelcast.servers}")
    private String hazelcastServers;

    @Bean
    public HazelcastInstance hazelcastInstance() {
        ClientConfig clientConfig = new ClientConfig();

        clientConfig.getNetworkConfig().addAddress(hazelcastServers.split(","));
        clientConfig.getNetworkConfig().setSmartRouting(false);

        return HazelcastClient.newHazelcastClient(clientConfig);
    }

    @Bean(name = "hazelcastCacheManager")
    public CacheManager hazelcastCacheManager() {
        return new HazelcastCacheManager(hazelcastInstance());
    }
}