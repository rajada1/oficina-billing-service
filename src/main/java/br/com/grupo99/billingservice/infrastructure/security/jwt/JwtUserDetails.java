package br.com.grupo99.billingservice.infrastructure.security.jwt;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

public class JwtUserDetails implements UserDetails {

    private final String username;
    private final String password;
    private final UUID pessoaId;
    private final String numeroDocumento;
    private final String tipoPessoa;
    private final String cargo;
    private final String perfil;
    private final Collection<? extends GrantedAuthority> authorities;

    public JwtUserDetails() {
        this.username = "";
        this.password = null;
        this.pessoaId = null;
        this.numeroDocumento = "";
        this.tipoPessoa = null;
        this.cargo = null;
        this.perfil = null;
        this.authorities = Collections.emptyList();
    }

    private JwtUserDetails(String username, String password, UUID pessoaId, String numeroDocumento,
            String tipoPessoa, String cargo, String perfil) {
        this.username = username;
        this.password = password;
        this.pessoaId = pessoaId;
        this.numeroDocumento = numeroDocumento;
        this.tipoPessoa = tipoPessoa;
        this.cargo = cargo;
        this.perfil = perfil;
        this.authorities = perfil != null
                ? Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + perfil))
                : Collections.emptyList();
    }

    public static JwtUserDetails from(String username, String pessoaId, String numeroDocumento,
            String tipoPessoa, String cargo, String perfil) {
        Objects.requireNonNull(username, "Username não pode ser nulo");
        Objects.requireNonNull(pessoaId, "PessoaId não pode ser nulo");
        Objects.requireNonNull(numeroDocumento, "Número de documento não pode ser nulo");
        Objects.requireNonNull(tipoPessoa, "Tipo de pessoa não pode ser nulo");
        Objects.requireNonNull(perfil, "Perfil não pode ser nulo");

        UUID pessoaUuid;
        try {
            pessoaUuid = UUID.fromString(pessoaId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("PessoaId inválido: " + pessoaId, e);
        }

        return new JwtUserDetails(username, null, pessoaUuid, numeroDocumento, tipoPessoa, cargo, perfil);
    }

    public static JwtUserDetails withPassword(String username, String password, String pessoaId,
            String numeroDocumento, String tipoPessoa,
            String cargo, String perfil) {
        Objects.requireNonNull(username, "Username não pode ser nulo");
        Objects.requireNonNull(password, "Password não pode ser nulo");
        Objects.requireNonNull(pessoaId, "PessoaId não pode ser nulo");
        Objects.requireNonNull(numeroDocumento, "Número de documento não pode ser nulo");
        Objects.requireNonNull(tipoPessoa, "Tipo de pessoa não pode ser nulo");
        Objects.requireNonNull(perfil, "Perfil não pode ser nulo");

        UUID pessoaUuid;
        try {
            pessoaUuid = UUID.fromString(pessoaId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("PessoaId inválido: " + pessoaId, e);
        }

        return new JwtUserDetails(username, password, pessoaUuid, numeroDocumento, tipoPessoa, cargo, perfil);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public String getPerfil() {
        return perfil;
    }

    public UUID getPessoaId() {
        return pessoaId;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public String getTipoPessoa() {
        return tipoPessoa;
    }

    public String getCargo() {
        return cargo;
    }

    public boolean isMecanicoOrHigher() {
        return perfil != null && ("MECANICO".equals(perfil) || "ADMIN".equals(perfil));
    }

    public boolean isCliente() {
        return perfil != null && "CLIENTE".equals(perfil);
    }

    public boolean isOwnerOrMecanico(UUID pessoaId) {
        if (isMecanicoOrHigher()) {
            return true;
        }

        if (this.pessoaId == null) {
            return false;
        }

        return this.pessoaId.equals(pessoaId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        JwtUserDetails that = (JwtUserDetails) o;
        return Objects.equals(username, that.username) &&
                Objects.equals(pessoaId, that.pessoaId) &&
                Objects.equals(numeroDocumento, that.numeroDocumento) &&
                Objects.equals(tipoPessoa, that.tipoPessoa) &&
                Objects.equals(cargo, that.cargo) &&
                Objects.equals(perfil, that.perfil);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, pessoaId, numeroDocumento, tipoPessoa, cargo, perfil);
    }

    @Override
    public String toString() {
        return "JwtUserDetails{" +
                "username='" + username + '\'' +
                ", pessoaId=" + pessoaId +
                ", numeroDocumento='" + numeroDocumento + '\'' +
                ", tipoPessoa=" + tipoPessoa +
                ", cargo='" + cargo + '\'' +
                ", perfil=" + perfil +
                '}';
    }
}
