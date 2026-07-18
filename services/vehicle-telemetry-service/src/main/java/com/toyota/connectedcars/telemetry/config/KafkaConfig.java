package com.toyota.connectedcars.telemetry.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String TELEMETRY_TOPIC = "vehicle.telemetry";

    @Bean
    public NewTopic telemetryTopic(@Value("${app.kafka.telemetry-partitions:6}") int partitions) {
        return TopicBuilder.name(TELEMETRY_TOPIC)
                .partitions(partitions)
                .replicas(1)
                .build();
    }
}
