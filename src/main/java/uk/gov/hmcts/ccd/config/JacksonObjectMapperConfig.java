package uk.gov.hmcts.ccd.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.config.MethodInvokingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonObjectMapperConfig {

    /**
     * An object mapper configured to support java.time and write Date and Times in ISO8601.
     *
     * @return Default ObjectMapper, used by Spring and HAL to serialise responses, and deserialise requests.
     */
    @Primary
    @Bean(name = "DefaultObjectMapper")
    public ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean(name = "SimpleObjectMapper")
    public ObjectMapper simpleObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    public MethodInvokingBean jsonPathParserForJackson() {
        MethodInvokingBean jsonPathParserForJackson = new MethodInvokingBean();
        jsonPathParserForJackson.setStaticMethod("uk.gov.hmcts.ccd.config.JaywayJsonPathConfigHelper.configureJsonPathForJackson");
        return jsonPathParserForJackson;
    }

}
