package br.com.grupo99.billingservice.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do OpenAPI (Swagger) para documentação da API de Faturamento.
 * Define o título, a versão e o esquema de segurança JWT.
 */
@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, bearerFormat = "JWT", scheme = "bearer")
@OpenAPIDefinition(info = @Info(title = "API de Faturamento", version = "v1", description = "API para gerenciamento de orçamentos e pagamentos da oficina. "
        +
        "Permite criar orçamentos, processar pagamentos e acompanhar o status financeiro.", contact = @Contact(name = "Grupo 99", email = "grupo99@fiap.com.br")), servers = {
                @Server(url = "/", description = "Default Server URL")
        }, security = @SecurityRequirement(name = "bearerAuth"))
public class OpenApiConfig {
}
