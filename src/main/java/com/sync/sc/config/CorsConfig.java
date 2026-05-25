package com.sync.sc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")   // 针对 /** 接口
                        .allowedOrigins("*")    // 允许任意源
                        .allowedMethods("GET", "POST", "PUT", "DELETE")
                        .allowCredentials(false);
            }
        };
    }
}
