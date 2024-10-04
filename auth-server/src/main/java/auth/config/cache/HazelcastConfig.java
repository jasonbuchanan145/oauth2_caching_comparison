package auth.config.cache;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastConfig {

        @Bean
        public HazelcastInstance hazelcastInstance() {
            ClientConfig clientConfig = new ClientConfig();
            clientConfig.getNetworkConfig().addAddress("hazelcast:5701");
            return HazelcastClient.newHazelcastClient(clientConfig);
        }

        @Bean(name = "hazelcastCacheManager")
        public CacheManager hazelcastCacheManager() {
            return new HazelcastCacheManager(hazelcastInstance());
        }
    }
