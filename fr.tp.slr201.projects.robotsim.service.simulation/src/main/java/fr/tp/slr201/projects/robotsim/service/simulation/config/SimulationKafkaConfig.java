package fr.tp.slr201.projects.robotsim.service.simulation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.tp.inf112.projects.robotsim.model.Factory;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class SimulationKafkaConfig {

    public static final String BOOTSTRAP_SERVERS = "localhost:9092";

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        return new KafkaAdmin(configs);
    }

    @Bean
    public AdminClient adminClient(final KafkaAdmin kafkaAdmin) {
        // KafkaAdmin exposes configuration via getConfigurationProperties()
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }

    @Bean
    public ProducerFactory<String, Factory> producerFactory(final ObjectMapper mapper) {
        final Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // Value serializer uses our primary ObjectMapper
        final JsonSerializer<Factory> factorySerializer = new JsonSerializer<>(mapper);
        factorySerializer.setAddTypeInfo(false);
        return new DefaultKafkaProducerFactory<>(config, new StringSerializer(), factorySerializer);
    }

    @Bean
    @Primary
    public KafkaTemplate<String, Factory> kafkaTemplate(final ProducerFactory<String, Factory> pf) {
        return new KafkaTemplate<>(pf);
    }
}
