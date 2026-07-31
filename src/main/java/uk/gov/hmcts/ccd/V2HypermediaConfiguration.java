package uk.gov.hmcts.ccd;

import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.mediatype.hal.HalMediaTypeConfiguration;
import org.springframework.hateoas.server.mvc.TypeConstrainedJacksonJsonHttpMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import tools.jackson.databind.json.JsonMapper;

/**
 * Keeps the service's existing HAL media-type handling on Jackson 3.
 */
@Configuration
public class V2HypermediaConfiguration implements WebMvcConfigurer {

    private static final MediaType VENDOR_JSON = MediaType.parseMediaType("application/*+json");

    private final JsonMapper jsonMapper;
    private final HalMediaTypeConfiguration halMediaTypeConfiguration;

    public V2HypermediaConfiguration(JsonMapper jsonMapper,
                                     HalMediaTypeConfiguration halMediaTypeConfiguration) {
        this.jsonMapper = jsonMapper;
        this.halMediaTypeConfiguration = halMediaTypeConfiguration;
    }

    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        var mediaTypes = new LinkedHashSet<>(halMediaTypeConfiguration.getMediaTypes());
        mediaTypes.add(MediaType.APPLICATION_JSON);
        mediaTypes.add(VENDOR_JSON);

        var halMapper = halMediaTypeConfiguration
            .configureJsonMapper(jsonMapper.rebuild())
            .build();
        HttpMessageConverter<?> converter = new TypeConstrainedJacksonJsonHttpMessageConverter(
            RepresentationModel.class,
            List.copyOf(mediaTypes),
            halMapper
        );
        builder.configureMessageConvertersList(converters -> converters.add(0, converter));
    }
}
