package edu.usd.jbuchanan.oauth.cache.comparison.auth.server.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;


public class CacheTypeFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        try {
            CacheTypeContext.setCacheType(((HttpServletRequest) request).getHeader("cache-type"));
            filterChain.doFilter(request, response);
        } finally {
            CacheTypeContext.clear();
        }
    }

}
