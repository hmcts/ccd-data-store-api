package uk.gov.hmcts.ccd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreCaseDataConfiguration {

    @Bean
    @Qualifier("caseDetailsObjectMapper")
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    @Qualifier("commonObjectMapper")
    public ObjectMapper commonObjectMapper() {
        return new ObjectMapper();
    }
}
