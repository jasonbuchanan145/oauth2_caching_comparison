package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config;

public class CacheTypeContext {
    private static final ThreadLocal<String> CACHE_TYPE = new ThreadLocal<>();

    public static void setCacheType(String cacheType) {
        CACHE_TYPE.set(cacheType);
    }

    public static String getCacheType() {
        return CACHE_TYPE.get();
    }

    public static void clear() {
        CACHE_TYPE.remove();
    }
}
