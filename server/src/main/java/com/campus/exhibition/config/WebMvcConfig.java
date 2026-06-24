package com.campus.exhibition.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * 静态资源映射 —— 将 /uploads/** 映射到本地文件存储目录
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.base-dir:./uploads}")
    private String baseDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path absolutePath = Path.of(baseDir).toAbsolutePath().normalize();
        String location = "file:" + absolutePath.toString().replace('\\', '/') + "/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
