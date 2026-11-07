package com.ticketflow.apigateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component   // s'exécute APRÈS Spring Security → le JWT est déjà validé
public class UserHeadersFilter extends OncePerRequestFilter {

    private static final String PREFIX = "x-user-";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Map<String, String> userHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken auth) {
            userHeaders.put("X-User-Email", auth.getToken().getClaimAsString("email"));
            userHeaders.put("X-User-Roles", auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).collect(Collectors.joining(",")));
        }
        chain.doFilter(new Wrapper(request, userHeaders), response);
    }

    /** Cache les X-User-* envoyés par le client, expose les nôtres. */
    private static class Wrapper extends HttpServletRequestWrapper {
        private final Map<String, String> userHeaders;

        Wrapper(HttpServletRequest request, Map<String, String> userHeaders) {
            super(request);
            this.userHeaders = userHeaders;
        }

        private boolean isUserHeader(String name) { return name.toLowerCase().startsWith(PREFIX); }

        @Override
        public String getHeader(String name) {
            return isUserHeader(name) ? userHeaders.get(name) : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (!isUserHeader(name)) return super.getHeaders(name);
            String value = userHeaders.get(name);
            return Collections.enumeration(value == null ? List.of() : List.of(value));
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = new ArrayList<>();
            Collections.list(super.getHeaderNames()).stream()
                    .filter(n -> !isUserHeader(n)).forEach(names::add);
            names.addAll(userHeaders.keySet());
            return Collections.enumeration(names);
        }
    }
}