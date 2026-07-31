package uk.gov.hmcts.ccd.config;

import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.config.MethodInvokingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;

@Configuration
public class JacksonObjectMapperConfig {

    /**
     * An object mapper configured to support java.time and write Date and Times in ISO8601.
     *
     * @return Default ObjectMapper, used by Spring and HAL to serialise responses, and deserialise requests.
     */
    @Primary
    @Bean(name = "DefaultObjectMapper")
    public JsonMapper defaultObjectMapper() {
        return JsonMapper.builderWithJackson2Defaults()
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(MapperFeature.DETECT_PARAMETER_NAMES)
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .addMixIn(Document.class, AmDocumentJacksonMixin.class)
            .build();
    }

    @Bean(name = "SimpleObjectMapper")
    public JsonMapper simpleObjectMapper() {
        return JsonMapper.builderWithJackson2Defaults().build();
    }

    @Bean
    public MethodInvokingBean jsonPathParserForJackson() {
        MethodInvokingBean jsonPathParserForJackson = new MethodInvokingBean();
        jsonPathParserForJackson
            .setStaticMethod("uk.gov.hmcts.ccd.config.JaywayJsonPathConfigHelper.configureJsonPathForJackson");
        return jsonPathParserForJackson;
    }

}
