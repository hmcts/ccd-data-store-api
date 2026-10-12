package uk.gov.hmcts.ccd.decentralised.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import feign.Feign;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.customheaders.CustomHeadersFilter;
import uk.gov.hmcts.ccd.decentralised.dto.DecentralisedCaseEvent;
import uk.gov.hmcts.ccd.decentralised.dto.DecentralisedEventDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.PersistenceStrategyResolver;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;
import uk.gov.hmcts.ccd.infrastructure.IdempotencyKeyHolder;
import uk.gov.hmcts.ccd.util.ClientContextUtil;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.callbacks.CallbackService.CLIENT_CONTEXT;

class ServicePersistenceClientContextTest {
    private static final String SUBMIT_PATH = "/ccd-persistence/cases";
    private static final String INCOMING = "{\"selectedPartyId\":\"party-1\",\"client_context\":{\"old\":true}}";
    private static final String RETURNED = "{\"client_context\":{\"user_task\":{\"complete_task\":true}}}";
    private static final String MERGED = """
        {"selectedPartyId":"party-1","client_context":{"user_task":{"complete_task":true}}}
        """;
    private static final String RESPONSE = """
        {
          "case_details": {
            "id": "1234567890123456", "case_type_id": "PCS", "jurisdiction": "PROBATE",
            "version": 1, "security_classification": "PUBLIC", "case_data": {}
          },
          "revision": 1
        }
        """;
    private final ObjectMapper mapper = new ObjectMapper();
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final ApplicationParams params = mock(ApplicationParams.class);
    private WireMockServer server;
    private ServicePersistenceClient client;
    private DecentralisedCaseEvent event;
    private UUID idempotencyKey;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(wireMockConfig().dynamicPort());
        server.start();
        var converters = new HttpMessageConverters(new MappingJackson2HttpMessageConverter(mapper));
        final var api = Feign.builder()
            .contract(new SpringMvcContract())
            .encoder(new SpringEncoder(() -> converters))
            .decoder(new ResponseEntityDecoder(new SpringDecoder(() -> converters)))
            .target(ServicePersistenceAPI.class, server.baseUrl());
        final var resolver = mock(PersistenceStrategyResolver.class);
        var keyHolder = new IdempotencyKeyHolder();
        keyHolder.computeAndSetKeyToRequestContext("event-token");
        idempotencyKey = keyHolder.getKey();
        var pointer = new CaseDetails();
        pointer.setReference(1234567890123456L);
        pointer.setCaseTypeId("PCS");
        pointer.setJurisdiction("PROBATE");
        pointer.setId("1");
        event = DecentralisedCaseEvent.builder().caseDetails(pointer)
            .eventDetails(DecentralisedEventDetails.builder().eventId("reply").build()).build();
        when(resolver.resolveUriOrThrow(pointer)).thenReturn(URI.create(server.baseUrl()));
        when(params.getCallbackPassthruHeaderContexts()).thenReturn(List.of(CLIENT_CONTEXT));
        client = new ServicePersistenceClient(api, resolver, keyHolder, request, params);
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @ParameterizedTest
    @ValueSource(ints = {201, 200})
    void forwardsContextAndReturnsMergedContextForCreationAndReplay(int status) throws Exception {
        request.addHeader(CLIENT_CONTEXT, INCOMING);
        stubResponse(status, RESPONSE, encoded(RETURNED));

        submitThroughFilter();

        verifySubmittedContext(INCOMING);
        assertEquals(mapper.readTree(MERGED),
            mapper.readTree(ClientContextUtil.decodeFromBase64(response.getHeader(CLIENT_CONTEXT))));
    }

    @Test
    void prefersLatestRequestAttributeAndPreservesItWhenResponseHasNoContext() throws Exception {
        request.addHeader(CLIENT_CONTEXT, INCOMING);
        String latest = encoded("{\"selectedPartyId\":\"party-2\"}");
        request.setAttribute(CLIENT_CONTEXT, latest);
        stubResponse(200, RESPONSE, null);

        submitThroughFilter();

        verifySubmittedContext(latest);
        assertEquals(latest, request.getAttribute(CLIENT_CONTEXT));
        assertEquals(latest, response.getHeader(CLIENT_CONTEXT));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = INCOMING)
    void preservesIncomingContextWhenResponseHasNoContext(String incoming) throws Exception {
        if (incoming != null) {
            request.addHeader(CLIENT_CONTEXT, incoming);
        }
        stubResponse(201, RESPONSE, null);

        submitThroughFilter();

        verifySubmittedContext(incoming);
        assertEquals(incoming, response.getHeader(CLIENT_CONTEXT));
    }

    @Test
    void returnsServiceContextWithoutIncomingContext() throws Exception {
        stubResponse(201, RESPONSE, "[" + encoded(RETURNED) + "]");

        submitThroughFilter();

        verifySubmittedContext(null);
        assertEquals(encoded(RETURNED), response.getHeader(CLIENT_CONTEXT));
    }

    @Test
    void respectsDisabledPassthroughConfiguration() throws Exception {
        when(params.getCallbackPassthruHeaderContexts()).thenReturn(List.of());
        request.addHeader(CLIENT_CONTEXT, INCOMING);
        stubResponse(201, RESPONSE, encoded(RETURNED));

        submitThroughFilter();

        verifySubmittedContext(null);
        assertNull(request.getAttribute(CLIENT_CONTEXT));
        assertNull(response.getHeader(CLIENT_CONTEXT));
    }

    @Test
    void usesConfiguredHeaderCasingForRequestAttributes() throws Exception {
        when(params.getCallbackPassthruHeaderContexts()).thenReturn(List.of("client-context"));
        String latest = encoded(INCOMING);
        request.setAttribute("client-context", latest);
        stubResponse(201, RESPONSE, encoded(RETURNED));

        submitThroughFilter();

        verifySubmittedContext(latest);
        assertEquals(request.getAttribute("client-context"), response.getHeader("client-context"));
    }

    @Test
    void retainsFunctionalErrorHandlingAndContextLikeNormalCallbacks() throws Exception {
        request.addHeader(CLIENT_CONTEXT, INCOMING);
        stubResponse(200, "{\"errors\":[\"Cannot submit\"]}", encoded(RETURNED));

        new CustomHeadersFilter(params).doFilter(request, response, (req, res) -> {
            assertThrows(ApiException.class, () -> client.createEvent(event));
            res.getWriter().write("validation error");
        });

        verifySubmittedContext(INCOMING);
        assertEquals(mapper.readTree(MERGED),
            mapper.readTree(ClientContextUtil.decodeFromBase64(response.getHeader(CLIENT_CONTEXT))));
        assertEquals("validation error", response.getContentAsString());
    }

    private void submitThroughFilter() throws Exception {
        new CustomHeadersFilter(params).doFilter(request, response, (req, res) -> {
            var saved = client.createEvent(event).getCaseDetails();
            assertEquals("1", saved.getId());
            assertEquals(1L, saved.getRevision());
            res.getWriter().write("saved");
        });
        assertEquals("saved", response.getContentAsString());
    }

    private void stubResponse(int status, String body, String context) {
        var result = aResponse().withStatus(status).withHeader("Content-Type", "application/json").withBody(body);
        if (context != null) {
            result.withHeader("client-context", context);
        }
        server.stubFor(post(urlEqualTo(SUBMIT_PATH)).willReturn(result));
    }

    private void verifySubmittedContext(String context) {
        server.verify(postRequestedFor(urlEqualTo(SUBMIT_PATH))
            .withHeader(CLIENT_CONTEXT, context == null ? absent() : equalTo(context))
            .withHeader("Idempotency-Key", equalTo(idempotencyKey.toString())));
    }

    private String encoded(String json) {
        return ClientContextUtil.encodeToBase64(json);
    }
}
