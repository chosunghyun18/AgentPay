package com.agentpay.agent.presentation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 이 핸들러는 에이전트 API 키 인증이 필요하다. {@link AgentAuthInterceptor}가 처리한다. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AgentAuthenticated {
}
