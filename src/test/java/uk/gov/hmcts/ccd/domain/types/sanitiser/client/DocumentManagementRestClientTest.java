package uk.gov.hmcts.ccd.domain.types.sanitiser.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.support.junit.NeedsServer;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.types.sanitiser.document.Binary;
import uk.gov.hmcts.ccd.domain.types.sanitiser.document.Document;
import uk.gov.hmcts.ccd.domain.types.sanitiser.document._links;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.builder.verify.VerifyHttp.verifyHttp;
import static com.xebialabs.restito.semantics.Action.contentType;
import static com.xebialabs.restito.semantics.Action.ok;
import static com.xebialabs.restito.semantics.Action.stringContent;
import static com.xebialabs.restito.semantics.Action.unauthorized;
import static com.xebialabs.restito.semantics.Condition.get;
import static com.xebialabs.restito.semantics.Condition.method;
import static com.xebialabs.restito.semantics.Condition.uri;
import static com.xebialabs.restito.semantics.Condition.withHeader;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DocumentManagementRestClientTest extends StubServerDependent {

    private static final JsonNodeFactory JSON_FACTORY = new JsonNodeFactory(false);
    private static final String RESOURCE_URI = "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0";
    private static final String DOCUMENT_URL = "http://localhost:%s" + RESOURCE_URI;
    private static final String BINARY_URL = "http://localhost:%s/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0/binary";
    private static final String FILENAME = "Seagulls_Sqaure.jpg";
    private static final String TYPE_DOCUMENT = "Document";
    private static final String DOCUMENT_FIELD_ID = "D8Document";
    private static final FieldTypeDefinition DOCUMENT_FIELD_TYPE = new FieldTypeDefinition();
    private static final CaseFieldDefinition DOCUMENT_FIELD = new CaseFieldDefinition();
    private static final CaseTypeDefinition CASE_TYPE = new CaseTypeDefinition();
    private static final ObjectNode DOCUMENT_VALUE_INITIAL = JSON_FACTORY.objectNode();
    private static final ObjectNode DOCUMENT_VALUE_SANITISED = JSON_FACTORY.objectNode();
    private static final String BEARER_TEST_JWT = "Bearer testJwt";
    private static final String SERVICE_JWT = "ey373478378347847834783784";

    private ObjectMapper objectMapper = new ObjectMapper();
    private RestTemplate restTemplate = new RestTemplate();

    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private Document document;
    private DocumentManagementRestClient subject;

    static {
        DOCUMENT_FIELD_TYPE.setId(TYPE_DOCUMENT);
        DOCUMENT_FIELD_TYPE.setType(TYPE_DOCUMENT);
        DOCUMENT_FIELD.setId(DOCUMENT_FIELD_ID);
        DOCUMENT_FIELD.setFieldTypeDefinition(DOCUMENT_FIELD_TYPE);

        CASE_TYPE.setCaseFieldDefinitions(Collections.singletonList(DOCUMENT_FIELD));
        DOCUMENT_VALUE_INITIAL.put("document_url", DOCUMENT_URL);
        DOCUMENT_VALUE_SANITISED.put("document_url", DOCUMENT_URL);
        DOCUMENT_VALUE_SANITISED.put("document_binary_url", BINARY_URL);
        DOCUMENT_VALUE_SANITISED.put("document_filename", FILENAME);
    }

    @Before
    public void setUp() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", BEARER_TEST_JWT);
        headers.add("ServiceAuthorization", SERVICE_JWT);
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        when(securityUtils.authorizationHeaders()).thenReturn(headers);

        document = new Document();
        _links links = new _links();
        Binary binary = new Binary();
        binary.setHref(formatURL(BINARY_URL));
        links.setBinary(binary);
        document.set_links(links);
        document.setOriginalDocumentName(FILENAME);
        subject = new DocumentManagementRestClient(securityUtils, restTemplate);
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.ALL));
        messageConverters.add(converter);
        restTemplate.setMessageConverters(messageConverters);
    }

    @Test
    @NeedsServer
    public void shouldGetDocument() throws Exception {
        // Document retrieval needs to work, regardless of the Content-Type header sent in Document Management's
        // response (it's different from the expected type of "application/json"). Therefore, the test mimics the server
        // sending a non-standard Content-Type header.
        whenHttp(server)
            .match(get(RESOURCE_URI),
                withHeader("Authorization", BEARER_TEST_JWT),
                withHeader("ServiceAuthorization", SERVICE_JWT),
                withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE))
            .then(ok(), jsonContent(document), contentType("application/ccd"));

        Document actual = subject.getDocument(DOCUMENT_FIELD_TYPE, formatURL(DOCUMENT_URL));

        assertEquals(FILENAME, actual.getOriginalDocumentName());
        assertEquals(formatURL(BINARY_URL), actual.get_links().getBinary().getHref());
        verifyHttp(server)
            .once(method(Method.GET),
                uri(RESOURCE_URI),
                withHeader("Authorization", BEARER_TEST_JWT),
                withHeader("ServiceAuthorization", SERVICE_JWT),
                withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE));
    }

    @Test(expected = ApiException.class)
    @NeedsServer
    public void shouldFailToSanitizeIfUnauthorized() throws Exception {

        whenHttp(server)
            .match(get(RESOURCE_URI),
                withHeader("Authorization", BEARER_TEST_JWT),
                withHeader("ServiceAuthorization", SERVICE_JWT),
                withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE))
            .then(unauthorized());

        subject.getDocument(DOCUMENT_FIELD_TYPE, formatURL(DOCUMENT_URL));

    }

    @Test(expected = ApiException.class)
    @NeedsServer
    public void shouldFailToSanitizeIfServiceUnavailable() throws Exception {

        whenHttp(server)
            .match(get(RESOURCE_URI),
                withHeader("Authorization", BEARER_TEST_JWT),
                withHeader("ServiceAuthorization", SERVICE_JWT),
                withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE))
            .then(r -> {
                r.setStatus(HttpStatus.SERVICE_UNAVAILABLE_503);
                return r;
            });

        subject.getDocument(DOCUMENT_FIELD_TYPE, formatURL(DOCUMENT_URL));
    }

    @Test(expected = ApiException.class)
    @NeedsServer
    public void shouldFailToSanitizeIfBadGateway() throws Exception {

        whenHttp(server)
            .match(get(RESOURCE_URI),
                withHeader("Authorization", BEARER_TEST_JWT),
                withHeader("ServiceAuthorization", SERVICE_JWT),
                withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE))
            .then(r -> {
                r.setStatus(HttpStatus.BAD_GATEWAY_502);
                return r;
            });

        subject.getDocument(DOCUMENT_FIELD_TYPE, formatURL(DOCUMENT_URL));
    }

    @Test(expected = ApiException.class)
    @NeedsServer
    public void shouldFailToSanitizeIfInternalServerError() throws Exception {

        whenHttp(server)
            .match(get(RESOURCE_URI),
                withHeader("Authorization", BEARER_TEST_JWT),
                withHeader("ServiceAuthorization", SERVICE_JWT),
                withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE))
            .then(r -> {
                r.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                return r;
            });

        subject.getDocument(DOCUMENT_FIELD_TYPE, formatURL(DOCUMENT_URL));
    }


    private String formatURL(String url) {
        return format(url, server.getPort());
    }

    private Action jsonContent(Object object) throws JsonProcessingException {
        return stringContent(objectMapper.writeValueAsString(object));
    }

}
