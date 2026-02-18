package br.com.grupo99.billingservice.infrastructure.config;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuração do SDK do Mercado Pago.
 *
 * Inicializa o access token globalmente e expõe PaymentClient e PreferenceClient
 * como beans Spring.
 */
@Slf4j
@Configuration
public class MercadoPagoConfiguration {

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @PostConstruct
    public void init() {
        if (accessToken != null && !accessToken.isBlank()) {
            MercadoPagoConfig.setAccessToken(accessToken);
            log.info("Mercado Pago SDK configurado com sucesso (token length={})", accessToken.length());
        } else {
            log.warn("Mercado Pago access token não configurado. Integração com MP desabilitada.");
        }
    }

    @Bean
    public PaymentClient paymentClient() {
        return new PaymentClient();
    }

    @Bean
    public PreferenceClient preferenceClient() {
        return new PreferenceClient();
    }
}
