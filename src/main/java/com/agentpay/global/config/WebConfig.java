package com.agentpay.global.config;

import com.agentpay.agent.presentation.AgentAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AgentAuthInterceptor agentAuthInterceptor;

    public WebConfig(AgentAuthInterceptor agentAuthInterceptor) {
        this.agentAuthInterceptor = agentAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(agentAuthInterceptor).addPathPatterns("/v1/**");
    }
}
