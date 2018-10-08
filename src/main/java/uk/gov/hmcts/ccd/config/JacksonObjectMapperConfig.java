package uk.gov.hmcts.ccd.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonObjectMapperConfig {

    @Primary
    @Bean(name = "HalObjectMapper")
    public ObjectMapper halObjectMapper() {
        ObjectMapper halObjectMapper = new ObjectMapper();
        halObjectMapper.registerModules(new JavaTimeModule());
        halObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return halObjectMapper;
    }

    @Bean(name = "SimpleObjectMapper")
    public ObjectMapper simpleObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
