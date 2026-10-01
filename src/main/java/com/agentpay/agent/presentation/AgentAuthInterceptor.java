package com.agentpay.agent.presentation;

import com.agentpay.agent.application.AgentAuthenticator;
import com.agentpay.agent.domain.AgentCredential;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * 에이전트 인증: {@link AgentAuthenticated}가 붙은 핸들러에 대해 {@code Authorization: Bearer ap_live_...} 검증.
 * 인증된 에이전트는 request attribute({@link #ATTRIBUTE})로 컨트롤러에 전달된다.
 *
 * <p>Phase 0 단순화: Spring Security 대신 인터셉터. 사람(소유자) 인증은 아직 스텁(X-Owner-Id 헤더)이며
 * Phase 1에서 Spring Security + 사람/에이전트 이중 principal로 교체한다.
 */
@Component
public class AgentAuthInterceptor implements HandlerInterceptor {

    public static final String ATTRIBUTE = "authenticatedAgent";
    private static final String BEARER = "Bearer ";

    private final AgentAuthenticator authenticator;

    public AgentAuthInterceptor(AgentAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod method) || !method.hasMethodAnnotation(AgentAuthenticated.class)) {
            return true;
        }
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        Optional<AgentCredential> agent = header != null && header.startsWith(BEARER)
                ? authenticator.authenticate(header.substring(BEARER.length()).trim())
                : Optional.empty();
        if (agent.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"유효한 에이전트 API 키가 필요합니다\"}");
            return false;
        }
        request.setAttribute(ATTRIBUTE, agent.get());
        return true;
    }
}
