package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CachedClientRepository implements RegisteredClientRepository {
        private final DynamicCacheResolver cacheManager;
        public static final String CLIENT_CACHE_NAME = "oauth2-clients";

        public CachedClientRepository(DynamicCacheResolver cacheManager) {
            this.cacheManager = cacheManager;
        }

        @Override
        public void save(RegisteredClient registeredClient) {
            if(CacheTypeContext.getCacheType()==null){
                log.info("Cannot resolve type, save it in all of them");
                cacheManager.getAll()
                        .forEach(manager ->{
                            log.info(manager.getClass().getName());
                            Cache clientCache =  manager.getCache(CLIENT_CACHE_NAME);
                    clientCache.put(registeredClient.getId(), registeredClient);
                    clientCache.put(registeredClient.getClientId(), registeredClient.getId());
                });
            }
            else {
                Cache clientCache = cacheManager.getCacheManager().getCache(CLIENT_CACHE_NAME);
                if (clientCache != null) {
                    clientCache.put(registeredClient.getId(), registeredClient);
                    clientCache.put(registeredClient.getClientId(), registeredClient.getId());
                }
            }
        }

        @Override
        public RegisteredClient findById(String id) {
            Cache clientCache = cacheManager.getCacheManager().getCache(CLIENT_CACHE_NAME);
            if (clientCache != null) {
                return clientCache.get(id, RegisteredClient.class);
            }
            return null;
        }

        @Override
        public RegisteredClient findByClientId(String clientId) {
            Cache clientCache = cacheManager.getCacheManager().getCache(CLIENT_CACHE_NAME);
            if (clientCache != null) {
                String id = clientCache.get(clientId, String.class);
                if (id != null) {
                    return findById(id);
                }
            }
            return null;
        }

}
