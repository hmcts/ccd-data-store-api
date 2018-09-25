package uk.gov.hmcts.ccd.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.ccd.config.JacksonObjectMapperConfig.HalObjectMapperPostProcessor.HAL_OBJECT_MAPPER;

@DisplayName("JacksonObjectMapperConfig")
class JacksonObjectMapperConfigTest {

    @Mock
    private ObjectMapper objectMapper;

    private JacksonObjectMapperConfig.HalObjectMapperPostProcessor halProcessor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        final JacksonObjectMapperConfig mapperConfig = new JacksonObjectMapperConfig();
        halProcessor = mapperConfig.halObjectMapperPostProcessor();
    }

    @Test
    @DisplayName("should return bean as is before initialisation")
    void postProcessBeforeInitialization() {
        final Object bean = halProcessor.postProcessBeforeInitialization(objectMapper, HAL_OBJECT_MAPPER);

        assertThat(bean, sameInstance(objectMapper));
        verifyNoMoreInteractions(bean);
    }

    @Test
    @DisplayName("should configure HAL Object Mapper after initialisation")
    void shouldConfigureHalAfterInit() {
        final Object bean = halProcessor.postProcessAfterInitialization(objectMapper, HAL_OBJECT_MAPPER);

        assertThat(bean, sameInstance(objectMapper));
        verify(objectMapper).registerModules(Mockito.any(JavaTimeModule.class));
        verify(objectMapper).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        verifyNoMoreInteractions(bean);
    }

    @Test
    @DisplayName("should return other beans as is after initialisation")
    void shouldReturnOtherBeansAsIs() {
        final Object bean = halProcessor.postProcessAfterInitialization(objectMapper, "otherBean");

        assertThat(bean, sameInstance(objectMapper));
        verifyNoMoreInteractions(bean);
    }

}
