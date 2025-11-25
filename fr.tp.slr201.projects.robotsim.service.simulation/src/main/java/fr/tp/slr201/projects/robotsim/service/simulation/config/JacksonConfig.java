package fr.tp.slr201.projects.robotsim.service.simulation.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        final ObjectMapper mapper = new ObjectMapper();

        final BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                // allow model package types
                .allowIfSubType("fr.tp.inf112.projects.robotsim")
                .allowIfSubType("fr.tp.slr201.projects.robotsim.service.simulation")
                .build();

        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        return mapper;
    }
}
