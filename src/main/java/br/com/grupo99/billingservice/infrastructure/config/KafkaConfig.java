package br.com.grupo99.billingservice.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuração do Apache Kafka para o Billing Service
 * Inclui Dead Letter Topics e retry com backoff exponencial.
 * 
 * Padrão: Saga Coreografada
 * - Consome: os-events, execution-events
 * - Produz: billing-events
 */
@Slf4j
@Configuration
@EnableKafka
public class KafkaConfig {

    // Topic names
    public static final String TOPIC_OS_EVENTS = "os-events";
    public static final String TOPIC_BILLING_EVENTS = "billing-events";
    public static final String TOPIC_EXECUTION_EVENTS = "execution-events";

    // Dead Letter Topics
    public static final String DLT_OS_EVENTS = "os-events.DLT";
    public static final String DLT_BILLING_EVENTS = "billing-events.DLT";
    public static final String DLT_EXECUTION_EVENTS = "execution-events.DLT";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroupId;

    // ===================== PRODUCER CONFIGURATION =====================

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Garantias de entrega
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // Performance tuning
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ===================== CONSUMER CONFIGURATION =====================

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // Offset reset strategy
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // Trusted packages for JSON deserialization
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES,
                "br.com.grupo99.billingservice.domain.events," +
                        "br.com.grupo99.osservice.application.events," +
                        "br.com.grupo99.*,java.util");

        // Performance
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 45000);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 15000);

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        // Error handler com DLT e backoff exponencial
        factory.setCommonErrorHandler(kafkaErrorHandler());

        return factory;
    }

    /**
     * Error Handler com Dead Letter Topic e Exponential Backoff
     */
    @Bean
    public CommonErrorHandler kafkaErrorHandler() {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate(),
                (record, ex) -> {
                    String dltTopic = record.topic() + ".DLT";
                    log.error("🔴 Enviando mensagem para DLT: {}. Erro: {}", dltTopic, ex.getMessage());
                    return new org.apache.kafka.common.TopicPartition(dltTopic, record.partition());
                });

        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxElapsedTime(30000L);
        backOff.setMaxInterval(16000L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(
                org.apache.kafka.common.errors.SerializationException.class,
                org.springframework.messaging.converter.MessageConversionException.class);

        return errorHandler;
    }

    // ===================== TOPIC CONFIGURATION =====================

    @Bean
    public NewTopic billingEventsTopic() {
        return TopicBuilder.name(TOPIC_BILLING_EVENTS)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", String.valueOf(30L * 24 * 60 * 60 * 1000)) // 30 days
                .config("cleanup.policy", "delete")
                .build();
    }

    // ===================== DEAD LETTER TOPICS =====================

    @Bean
    public NewTopic osEventsDltTopic() {
        return TopicBuilder.name(DLT_OS_EVENTS)
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "604800000") // 7 dias
                .build();
    }

    @Bean
    public NewTopic billingEventsDltTopic() {
        return TopicBuilder.name(DLT_BILLING_EVENTS)
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "604800000")
                .build();
    }

    @Bean
    public NewTopic executionEventsDltTopic() {
        return TopicBuilder.name(DLT_EXECUTION_EVENTS)
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "604800000")
                .build();
    }
}
