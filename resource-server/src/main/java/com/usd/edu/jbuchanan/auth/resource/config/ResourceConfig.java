package com.usd.edu.jbuchanan.auth.resource.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
        "com.usd.edu.jbuchanan.auth.resource.config",
        "com.usd.edu.jbuchanan.auth.resource"
})
public class ResourceConfig {
}
