package kr.co.choi.property_manager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        String location = "file:" + (uploadDir.endsWith("/") ? uploadDir : uploadDir + "/");

        // ✅ 현재 DB에 /upload/... 로 저장된 값 대응
        registry.addResourceHandler("/upload/**")
                .addResourceLocations(location);

        // ✅ 앞으로 /uploads/... 로 통일할 때도 대응
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
