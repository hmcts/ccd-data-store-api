package uk.gov.hmcts.ccd.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@DisplayName("HalConfig")
class HalConfigTest {

    private HalConfig halConfig;

    @BeforeEach
    void setUp() {
        halConfig = new HalConfig();
    }

    @Nested
    @DisplayName("HalConverterPostProcessor")
    class HalConverterPostProcessorTest {
        private static final String BEAN_NAME = "Whatever";

        @Mock
        private RestTemplate restTemplateBean;

        @Mock
        private RequestMappingHandlerAdapter requestAdapterBean;

        @Mock
        private TypeConstrainedMappingJackson2HttpMessageConverter halConverter;

        @Mock
        private HttpMessageConverter<?> otherConverter;

        @Captor
        private ArgumentCaptor<List<MediaType>> mediaTypesCaptor;

        private HalConfig.HalConverterPostProcessor halProcessor;

        @BeforeEach
        void setUp() {
            MockitoAnnotations.initMocks(this);

            final List<HttpMessageConverter<?>> messageConverters = Arrays.asList(halConverter, otherConverter);
            when(restTemplateBean.getMessageConverters()).thenReturn(messageConverters);
            when(requestAdapterBean.getMessageConverters()).thenReturn(messageConverters);

            when(halConverter.canWrite(RepresentationModel.class, MediaTypes.HAL_JSON)).thenReturn(true);

            halProcessor = halConfig.halConverterPostProcessor();
        }

        @Test
        @DisplayName("should not alter other beans")
        void shouldNotAlterOtherBeans() {
            final Object otherBean = mock(Object.class);

            halProcessor.postProcessBeforeInitialization(otherBean, BEAN_NAME);
            halProcessor.postProcessAfterInitialization(otherBean, BEAN_NAME);

            verifyZeroInteractions(otherBean);
        }

        @Test
        @DisplayName("should not alter RestTemplate beans before init")
        void shouldNotAlterRestTemplateBeansBeforeInit() {
            halProcessor.postProcessBeforeInitialization(restTemplateBean, BEAN_NAME);

            verifyZeroInteractions(restTemplateBean);
        }

        @Test
        @DisplayName("should not alter RequestAdapter beans before init")
        void shouldNotAlterRequestAdapterBeansBeforeInit() {
            halProcessor.postProcessBeforeInitialization(requestAdapterBean, BEAN_NAME);

            verifyZeroInteractions(requestAdapterBean);
        }

        @Test
        @DisplayName("should add custom media types to HAL message converter for RestTemplate bean")
        void shouldAlterRestTemplateBeansAfterInit() {
            halProcessor.postProcessAfterInitialization(restTemplateBean, BEAN_NAME);

            verifyMediaTypes();
        }

        @Test
        @DisplayName("should add custom media types to HAL message converter for RequestAdepater bean")
        void shouldAlterRequestAdepaterBeansAfterInit() {
            halProcessor.postProcessAfterInitialization(restTemplateBean, BEAN_NAME);

            verifyMediaTypes();
        }

        @Test
        @DisplayName("should not alter other message converters for ResTemplate bean")
        void shouldNotAlterRestTemplateOtherMessageConverters() {
            halProcessor.postProcessAfterInitialization(restTemplateBean, BEAN_NAME);

            verifyZeroInteractions(otherConverter);
        }

        @Test
        @DisplayName("should not alter other message converters for RequestAdapter bean")
        void shouldNotAlterRequestAdapterOtherMessageConverters() {
            halProcessor.postProcessAfterInitialization(requestAdapterBean, BEAN_NAME);

            verifyZeroInteractions(otherConverter);
        }

        private void verifyMediaTypes() {
            verify(halConverter).setSupportedMediaTypes(mediaTypesCaptor.capture());
            final List<MediaType> mediaTypes = mediaTypesCaptor.getValue();
            assertAll(
                () -> assertThat(mediaTypes, hasSize(5)),
                () -> assertThat(mediaTypes, hasItem(MediaTypes.HAL_JSON)),
                () -> assertThat(mediaTypes, hasItem(HalConfig.APPLICATION_HAL_JSON_EXTENDED)),
                () -> assertThat(mediaTypes, hasItem(HalConfig.APPLICATION_HAL_JSON_EXTENDED_UTF8)),
                () -> assertThat(mediaTypes, hasItem(HalConfig.APPLICATION_JSON_EXTENDED)),
                () -> assertThat(mediaTypes, hasItem(HalConfig.APPLICATION_JSON_EXTENDED_UTF8))
            );
        }
    }
}
