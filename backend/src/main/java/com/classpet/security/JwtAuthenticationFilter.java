package com.classpet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        log.debug("[JWT Filter] {} {} - Auth header: {}", request.getMethod(), uri, 
            request.getHeader("Authorization") != null ? "present" : "missing");
        String token = getJwtFromRequest(request);
        if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
            log.debug("[JWT Filter] Token valid for URI: {}", uri);
            String username = tokenProvider.getUsernameFromToken(token);
            String teacherId = tokenProvider.getTeacherIdFromToken(token);
            String role = tokenProvider.getRoleFromToken(token);
            String studentId = tokenProvider.getStudentIdFromToken(token);
            
            AuthenticatedUser principal;
            if ("student".equals(role)) {
                principal = new AuthenticatedUser(username, teacherId, "student", studentId);
            } else {
                principal = new AuthenticatedUser(username, teacherId, "teacher", null);
            }
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, 
                        "student".equals(role) 
                            ? List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
                            : List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // 滑动过期：签发新 Token，通过响应头返回给前端
            // 学生用 generateStudentToken（包含 studentId），老师用 generateToken
            String newToken = "student".equals(role)
                    ? tokenProvider.generateStudentToken(username, teacherId, studentId)
                    : tokenProvider.generateToken(username, teacherId, role);
            response.setHeader("X-New-Token", newToken);
        }
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    public record AuthenticatedUser(String username, String teacherId, String role, String studentId) {
        public boolean isStudent() { return "student".equals(role); }
    }
}