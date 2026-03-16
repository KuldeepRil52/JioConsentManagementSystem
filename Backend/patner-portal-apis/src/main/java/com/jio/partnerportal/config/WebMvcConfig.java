package com.jio.partnerportal.config;

import com.jio.partnerportal.interceptor.LogInterceptor;
import com.jio.partnerportal.repository.TransactionLogRepository;
import com.jio.partnerportal.util.Validation;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
public class WebMvcConfig implements WebMvcConfigurer {
   Validation validation;
   TransactionLogRepository transactionLogRepository;

    public WebMvcConfig(Validation validation,TransactionLogRepository transactionLogRepository){
       this.validation=validation;
       this.transactionLogRepository=transactionLogRepository;

    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this.logInterceptor())
                .excludePathPatterns(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**"
                );

    }

    public LogInterceptor logInterceptor() {
        return new LogInterceptor(validation, transactionLogRepository);
    }
}
