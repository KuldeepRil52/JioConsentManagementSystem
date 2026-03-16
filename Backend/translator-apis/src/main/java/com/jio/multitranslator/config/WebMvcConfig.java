package com.jio.multitranslator.config;

import com.jio.multitranslator.interceptor.LogInterceptor;
import com.jio.multitranslator.utils.Validation;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for interceptors and other web-related settings.
 */
@Configuration
@EnableWebMvc
public class WebMvcConfig implements WebMvcConfigurer {

    private final Validation validation;

    public WebMvcConfig(Validation validation) {
        this.validation = validation;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this.logInterceptor())
                .excludePathPatterns(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/count",
                        "/getConfig*"
                );
    }

    public LogInterceptor logInterceptor() {
        return new LogInterceptor(validation);
    }

}
