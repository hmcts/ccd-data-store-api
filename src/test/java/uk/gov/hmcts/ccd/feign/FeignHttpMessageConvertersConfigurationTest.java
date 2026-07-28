package uk.gov.hmcts.ccd.feign;

import java.util.List;
import java.util.Map;

import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.openfeign.support.FeignHttpMessageConverters;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.ccd.config.JacksonObjectMapperConfig;

import static org.assertj.core.api.Assertions.assertThat;

class FeignHttpMessageConvertersConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            JacksonAutoConfiguration.class,
            HttpMessageConvertersAutoConfiguration.class
        ))
        .withUserConfiguration(
            JacksonObjectMapperConfig.class,
            FeignHttpMessageConvertersConfiguration.class
        );

    @Test
    void shouldEagerlyInitializeCcdJackson3ConvertersBeforePublishingBean() {
        contextRunner.run(context -> {
            FeignHttpMessageConverters feignConverters = context.getBean(FeignHttpMessageConverters.class);

            Object initializedConverters = ReflectionTestUtils.getField(feignConverters, "converters");
            assertThat(initializedConverters).isInstanceOf(List.class);

            @SuppressWarnings("unchecked")
            List<HttpMessageConverter<?>> converterList =
                (List<HttpMessageConverter<?>>) initializedConverters;
            assertThat(converterList).isNotEmpty();

            List<HttpMessageConverter<?>> jsonConverters = converterList.stream()
                .filter(converter -> converter.canWrite(Map.class, MediaType.APPLICATION_JSON))
                .toList();
            assertThat(jsonConverters).singleElement().isExactlyInstanceOf(JacksonJsonHttpMessageConverter.class);

            JacksonJsonHttpMessageConverter jacksonConverter =
                (JacksonJsonHttpMessageConverter) jsonConverters.getFirst();
            assertThat(jacksonConverter.getMapper())
                .isSameAs(context.getBean("DefaultObjectMapper", JsonMapper.class));
        });
    }
}
