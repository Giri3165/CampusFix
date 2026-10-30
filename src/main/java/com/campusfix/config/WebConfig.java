package com.campusfix.config;

import com.campusfix.service.FileStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Serves uploaded images from /uploads/** (login required, see SecurityConfig). */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final FileStorageService storageService;

    public WebConfig(FileStorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(storageService.getRoot().toUri().toString());
    }
}
