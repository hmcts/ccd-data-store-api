package uk.gov.hmcts.ccd.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

@Configuration
public class HalConfig {

    public static final String APPLICATION_JSON_EXTENDED_VALUE = "application/*+json";
    public static final MediaType APPLICATION_JSON_EXTENDED = MediaType.valueOf(APPLICATION_JSON_EXTENDED_VALUE);
    public static final String APPLICATION_JSON_EXTENDED_UTF8_VALUE =
        APPLICATION_JSON_EXTENDED_VALUE + ";charset=UTF-8";
    public static final MediaType APPLICATION_JSON_EXTENDED_UTF8 = MediaType.valueOf(
        APPLICATION_JSON_EXTENDED_UTF8_VALUE);
    public static final String APPLICATION_HAL_JSON_EXTENDED_VALUE = "application/*+hal+json";
    public static final MediaType APPLICATION_HAL_JSON_EXTENDED =
        MediaType.valueOf(APPLICATION_HAL_JSON_EXTENDED_VALUE);
    public static final String APPLICATION_HAL_JSON_EXTENDED_UTF8_VALUE =
        APPLICATION_HAL_JSON_EXTENDED_VALUE + ";charset=UTF-8";
    public static final MediaType APPLICATION_HAL_JSON_EXTENDED_UTF8 = MediaType.valueOf(
        APPLICATION_HAL_JSON_EXTENDED_UTF8_VALUE);

    private static final MediaType[] HAL_MEDIA_TYPES = new MediaType[]{
        MediaTypes.HAL_JSON,
        APPLICATION_JSON_EXTENDED,
        APPLICATION_JSON_EXTENDED_UTF8,
        APPLICATION_HAL_JSON_EXTENDED,
        APPLICATION_HAL_JSON_EXTENDED_UTF8
    };

    @Bean
    public HalConverterPostProcessor halConverterPostProcessor() {
        return new HalConverterPostProcessor();
    }

    /**
     * Given Spring HATEOAS v0.25.0.RELEASE does not support HAL with vendor media types,
     * those have to be enabled manually. This is adding the following media types to HAL:
     * application/*+json, application/*+json;charset=UTF-8, application/*+hal+json
     * and application/*+hal+json;charset=UTF-8.
     */
    class HalConverterPostProcessor implements BeanPostProcessor {

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) {
            return bean;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {

            if (bean instanceof RequestMappingHandlerAdapter) {
                RequestMappingHandlerAdapter adapter = (RequestMappingHandlerAdapter) bean;
                enableCustomMediaTypesForHal(adapter.getMessageConverters());
            }

            if (bean instanceof RestTemplate) {
                RestTemplate template = (RestTemplate) bean;
                enableCustomMediaTypesForHal(template.getMessageConverters());
            }

            return bean;
        }

        private void enableCustomMediaTypesForHal(List<HttpMessageConverter<?>> converters) {
            findHalConverters(converters)
                .forEach(converter -> {
                    converter.setSupportedMediaTypes(Arrays.asList(HAL_MEDIA_TYPES));
                });
        }

        private Stream<MappingJackson2HttpMessageConverter> findHalConverters(
            List<HttpMessageConverter<?>> converters) {
            return converters.stream()
                             .filter(converter ->
                                 converter instanceof TypeConstrainedMappingJackson2HttpMessageConverter
                                 && converter.canWrite(RepresentationModel.class, MediaTypes.HAL_JSON))
                             .map(converter -> (MappingJackson2HttpMessageConverter) converter);
        }
    }
}
