package com.misterworld.server.security;

import lombok.RequiredArgsConstructor;

import com.misterworld.server.repository.MemberRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.time.Clock;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtProvider provider;
    private final TokenSessionRepository sessions;
    private final MemberRepository members;
    private final Clock clock;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        var header = request.getHeader("Authorization");
        if (header != null) {
            try {
                if (!header.startsWith("Bearer "))
                    throw new IllegalArgumentException();
                var claims = provider.validate(header.substring(7), "ACCESS");
                var session = sessions.findById(claims.getStringClaim("sid")).orElseThrow();
                var member = members.findById(Long.valueOf(claims.getSubject())).orElseThrow();
                if (session.isRevoked() || !session.getExpiresAt().isAfter(clock.instant()) || !session.getMemberId()
                    .equals(member.getId()))
                    throw new IllegalArgumentException();
                var auth = new UsernamePasswordAuthenticationToken(member.getId().toString(), session.getId(),
                    List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole().name())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception error) {
                SecurityContextHolder.clearContext();
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                    "{\"success\":false,\"message\":\"유효한 인증 토큰이 필요합니다.\",\"status\":401,\"code\":\"INVALID_TOKEN\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
