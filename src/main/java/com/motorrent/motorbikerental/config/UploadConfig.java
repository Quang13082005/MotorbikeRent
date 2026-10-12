package com.motorrent.motorbikerental.config;
@org.springframework.context.annotation.Configuration
public class UploadConfig implements org.springframework.web.servlet.config.annotation.WebMvcConfigurer {
 @Override public void addResourceHandlers(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry r) {
  r.addResourceHandler("/uploads/avatars/**").addResourceLocations(java.nio.file.Path.of("uploads/avatars").toAbsolutePath().toUri().toString()+"/");
 }
}
