package com.greengo.config;

import com.greengo.interceptors.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor).excludePathPatterns(
                "/",
                "/healthz",
                "/admin-ui",
                "/admin-ui/**",
                "/user/login",
                "/user/register",
                "/admin/login",
                "/scooter/list",
                "/store/list",
                "/store/*"
        );
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/admin-ui").setViewName("forward:/admin-ui/index.html");
        registry.addViewController("/admin-ui/").setViewName("forward:/admin-ui/index.html");
        // Current admin routes are single-segment paths like /admin-ui/login.
        registry.addViewController("/admin-ui/{path:[^.]+}")
                .setViewName("forward:/admin-ui/index.html");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "https://prod-4g7i1ww2f71d4f7b-1422183264.tcloudbaseapp.com"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

