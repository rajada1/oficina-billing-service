package br.com.grupo99.billingservice.infrastructure.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@ConditionalOnProperty(name = "security.disabled", havingValue = "false", matchIfMissing = true)
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration.ms}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractPessoaId(String token) {
        String pessoaId = extractClaim(token, claims -> claims.get("pessoaId", String.class));
        if (pessoaId == null || pessoaId.trim().isEmpty()) {
            throw new IllegalArgumentException("Token JWT não contém claim 'pessoaId'");
        }
        return pessoaId;
    }

    public String extractNumeroDocumento(String token) {
        String numeroDocumento = extractClaim(token, claims -> claims.get("numeroDocumento", String.class));
        if (numeroDocumento == null || numeroDocumento.trim().isEmpty()) {
            throw new IllegalArgumentException("Token JWT não contém claim 'numeroDocumento'");
        }
        return numeroDocumento;
    }

    public String extractTipoPessoa(String token) {
        String tipoPessoa = extractClaim(token, claims -> claims.get("tipoPessoa", String.class));
        if (tipoPessoa == null || tipoPessoa.trim().isEmpty()) {
            throw new IllegalArgumentException("Token JWT não contém claim 'tipoPessoa'");
        }
        return tipoPessoa;
    }

    public String extractCargo(String token) {
        return extractClaim(token, claims -> claims.get("cargo", String.class));
    }

    public String extractPerfil(String token) {
        String perfil = extractClaim(token, claims -> claims.get("perfil", String.class));
        if (perfil == null || perfil.trim().isEmpty()) {
            throw new IllegalArgumentException("Token JWT não contém claim 'perfil'");
        }
        return perfil;
    }

    public JwtUserDetails extractUserDetails(String token) {
        String username = extractUsername(token);
        String pessoaId = extractPessoaId(token);
        String numeroDocumento = extractNumeroDocumento(token);
        String tipoPessoa = extractTipoPessoa(token);
        String cargo = extractCargo(token);
        String perfil = extractPerfil(token);

        return JwtUserDetails.from(username, pessoaId, numeroDocumento, tipoPessoa, cargo, perfil);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] decodedKey = java.util.Base64.getDecoder().decode(secretKey);
        return Keys.hmacShaKeyFor(decodedKey);
    }
}
