package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CacheTypeFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        String requestURI = ((HttpServletRequest)request).getRequestURI();

        if (requestURI.startsWith("/oauth/redis/")) {
            request.setAttribute("CACHE_TYPE", "redis");
        } else if (requestURI.startsWith("/oauth/memcached/")) {
            request.setAttribute("CACHE_TYPE", "memcached");
        } else if (requestURI.startsWith("/oauth/hazelcast/")) {
            request.setAttribute("CACHE_TYPE", "hazelcast");
        }

        filterChain.doFilter(request, response);
    }

}