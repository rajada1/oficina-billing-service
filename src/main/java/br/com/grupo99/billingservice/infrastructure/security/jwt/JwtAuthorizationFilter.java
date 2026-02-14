package br.com.grupo99.billingservice.infrastructure.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@ConditionalOnProperty(name = "security.disabled", havingValue = "false", matchIfMissing = true)
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthorizationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String perfil = jwtUtil.extractPerfil(jwt);
        String pessoaId = jwtUtil.extractPessoaId(jwt);

        if ("MECANICO".equals(perfil) || "ADMIN".equals(perfil)) {
            filterChain.doFilter(request, response);
            return;
        }

        if ("CLIENTE".equals(perfil)) {
            String requestURI = request.getRequestURI();
            String method = request.getMethod();

            if ("/api/v1/faturas".equals(requestURI) || "/api/v1/billing".equals(requestURI)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Acesso negado. Clientes não podem listar todos os itens.\"}");
                return;
            }

            if (!"GET".equals(method)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter()
                        .write("{\"error\":\"Acesso negado. Clientes só podem consultar faturas.\"}");
                return;
            }

            request.setAttribute("pessoaId", pessoaId);
            request.setAttribute("perfil", perfil);
        }

        filterChain.doFilter(request, response);
    }
}
