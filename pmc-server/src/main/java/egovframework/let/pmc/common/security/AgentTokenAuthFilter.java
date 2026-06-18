package egovframework.let.pmc.common.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Agent API 토큰 인증 필터.
 * Authorization: Bearer &lt;apiKey&gt; 또는 X-PMC-Api-Key 헤더의 키를 검증하여
 * 인증된 경우 principal=agentId, 권한 ROLE_AGENT 로 SecurityContext 설정.
 * 등록(/register) 등 토큰 없는 요청은 그대로 통과(보안설정에서 permitAll).
 */
public class AgentTokenAuthFilter extends OncePerRequestFilter {

    @Autowired
    private ApiKeyService apiKeyService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String key = extractKey(request);
        if (key != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            String agentId = apiKeyService.resolveAgentId(key);
            if (agentId != null) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        agentId, null, AuthorityUtils.createAuthorityList("ROLE_AGENT"));
                SecurityContextHolder.getContext().setAuthentication(auth);
                request.setAttribute("pmc.agentId", agentId);
            }
        }
        chain.doFilter(request, response);
    }

    private String extractKey(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7).trim();
        }
        String h = request.getHeader("X-PMC-Api-Key");
        return (h != null && !h.isEmpty()) ? h.trim() : null;
    }
}
