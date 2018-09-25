package uk.gov.hmcts.ccd.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonObjectMapperConfig {

    @Bean
    public HalObjectMapperPostProcessor halObjectMapperPostProcessor() {
        return new HalObjectMapperPostProcessor();
    }

    /**
     * Manual configuration of Jackson's ObjectMapper is currently required while Spring HATEOAS EnableHypermediaSupport
     * feature is used. For more information see https://github.com/spring-projects/spring-hateoas/issues/705
     */
    class HalObjectMapperPostProcessor implements BeanPostProcessor {

        static final String HAL_OBJECT_MAPPER = "_halObjectMapper";

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) {
            return bean;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            if (!HAL_OBJECT_MAPPER.equals(beanName)) {
                return bean;
            }

            final ObjectMapper halObjectMapper = (ObjectMapper) bean;

            /*
                Write java.time dates and times as ISO 8601
             */
            halObjectMapper.registerModules(new JavaTimeModule());
            halObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            return halObjectMapper;
        }
    }
}
