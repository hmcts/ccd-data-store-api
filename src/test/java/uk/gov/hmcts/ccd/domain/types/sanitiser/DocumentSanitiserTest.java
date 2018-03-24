package uk.gov.hmcts.ccd.domain.types.sanitiser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.ccd.domain.types.sanitiser.client.DocumentManagementRestClient;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.types.sanitiser.document.Binary;
import uk.gov.hmcts.ccd.domain.types.sanitiser.document.Document;
import uk.gov.hmcts.ccd.domain.types.sanitiser.document._links;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.types.sanitiser.DocumentSanitiser.DOCUMENT_BINARY_URL;
import static uk.gov.hmcts.ccd.domain.types.sanitiser.DocumentSanitiser.DOCUMENT_FILENAME;
import static uk.gov.hmcts.ccd.domain.types.sanitiser.DocumentSanitiser.DOCUMENT_URL;

@RunWith(MockitoJUnitRunner.class)
public class DocumentSanitiserTest {

    private static final JsonNodeFactory JSON_FACTORY = new JsonNodeFactory(false);

    private static final CaseType CASE_TYPE = new CaseType();

    private static final String TYPE_DOCUMENT = "Document";
    private static final FieldType DOCUMENT_FIELD_TYPE = new FieldType();
    private static final String DOCUMENT_FIELD_ID = "D8Document";
    private static final CaseField DOCUMENT_FIELD = new CaseField();
    private static final ObjectNode DOCUMENT_VALUE_INITIAL = JSON_FACTORY.objectNode();
    private static final ObjectNode DOCUMENT_VALUE_SANITISED = JSON_FACTORY.objectNode();
    private static final String DOCUMENT_URL_VALUE = "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0";
    private static final String BINARY_URL = "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d0/binary";
    private static final String FILENAME = "Seagulls_Sqaure.jpg";

    static {
        DOCUMENT_FIELD_TYPE.setId(TYPE_DOCUMENT);
        DOCUMENT_FIELD_TYPE.setType(TYPE_DOCUMENT);
        DOCUMENT_FIELD.setId(DOCUMENT_FIELD_ID);
        DOCUMENT_FIELD.setFieldType(DOCUMENT_FIELD_TYPE);

        CASE_TYPE.setCaseFields(Collections.singletonList(DOCUMENT_FIELD));
    }

    @Mock
    private DocumentManagementRestClient documentManagementRestClient;

    private DocumentSanitiser documentSanitiser;

    @Before
    public void setUp() throws Exception {
        documentSanitiser = new DocumentSanitiser(documentManagementRestClient);
        DOCUMENT_VALUE_INITIAL.put("document_url", DOCUMENT_URL_VALUE);
        DOCUMENT_VALUE_SANITISED.put("document_url", DOCUMENT_URL_VALUE);
        DOCUMENT_VALUE_SANITISED.put("document_binary_url",BINARY_URL);
        DOCUMENT_VALUE_SANITISED.put("document_filename", FILENAME);
    }

    @Test
    public void shouldSanitizeValidDocument() throws Exception {
        final Document document = buildDocument(BINARY_URL);
        document.setOriginalDocumentName(FILENAME);
        when(documentManagementRestClient.getDocument(DOCUMENT_FIELD_TYPE, DOCUMENT_URL_VALUE)).thenReturn(document);

        JsonNode sanitisedDocument = documentSanitiser.sanitise(DOCUMENT_FIELD_TYPE, DOCUMENT_VALUE_INITIAL);

        assertThat(sanitisedDocument, is(DOCUMENT_VALUE_SANITISED));
    }

    @Test
    public void shouldNotSanitizeIfDocumentSanitizedAlready() throws Exception {
        final JsonNode documentValue = JSON_FACTORY.objectNode();
        ((ObjectNode)documentValue).set(DOCUMENT_URL, JSON_FACTORY.textNode("testUrl"));
        ((ObjectNode)documentValue).set(DOCUMENT_BINARY_URL, JSON_FACTORY.textNode("testBinaryUrl"));
        ((ObjectNode)documentValue).set(DOCUMENT_FILENAME, JSON_FACTORY.textNode("testFilename"));

        JsonNode sanitisedDocument = documentSanitiser.sanitise(DOCUMENT_FIELD_TYPE, documentValue);

        assertAll(
            () -> verify(documentManagementRestClient, never()).getDocument(any(FieldType.class), anyString()),
            () -> assertThat(sanitisedDocument, is(documentValue))
        );
    }

    @Test(expected = ValidationException.class)
    public void shouldFailToSanitizeIfDocumentRetrievedButMissingBinaryLink() throws Exception {
        Document document = new Document();
        _links links = new _links();
        document.set_links(links);
        document.setOriginalDocumentName(FILENAME);
        when(documentManagementRestClient.getDocument(DOCUMENT_FIELD_TYPE, DOCUMENT_URL_VALUE)).thenReturn(document);

        documentSanitiser.sanitise(DOCUMENT_FIELD_TYPE, DOCUMENT_VALUE_INITIAL);
    }

    @Test(expected = ValidationException.class)
    public void shouldFailToSanitizeIfDocumentRetrievedButEmptyBinaryLink() throws Exception {
        final Document document = buildDocument();
        document.setOriginalDocumentName(FILENAME);
        when(documentManagementRestClient.getDocument(DOCUMENT_FIELD_TYPE, DOCUMENT_URL_VALUE)).thenReturn(document);

        documentSanitiser.sanitise(DOCUMENT_FIELD_TYPE, DOCUMENT_VALUE_INITIAL);
    }

    @Test(expected = ValidationException.class)
    public void shouldFailToSanitizeIfDocumentRetrievedButEmptyStringBinaryLink() throws Exception {
        final Document document = buildDocument("");
        document.setOriginalDocumentName(FILENAME);
        when(documentManagementRestClient.getDocument(DOCUMENT_FIELD_TYPE, DOCUMENT_URL_VALUE)).thenReturn(document);

        documentSanitiser.sanitise(DOCUMENT_FIELD_TYPE, DOCUMENT_VALUE_INITIAL);
    }

    @Test(expected = ValidationException.class)
    public void shouldFailToSanitizeIfDocumentRetrievedButNullBinaryLink() throws Exception {
        final Document document = buildDocument(null);
        document.setOriginalDocumentName(FILENAME);
        when(documentManagementRestClient.getDocument(DOCUMENT_FIELD_TYPE, DOCUMENT_URL_VALUE)).thenReturn(document);

        documentSanitiser.sanitise(DOCUMENT_FIELD_TYPE, DOCUMENT_VALUE_INITIAL);
    }

    @Test(expected = ValidationException.class)
    public void shouldFailToSanitizeIfDocumentRetrievedButMissingDocumentFilename() throws Exception {
        final Document document = buildDocument(BINARY_URL);
        when(documentManagementRestClient.getDocument(DOCUMENT_FIELD_TYPE, DOCUMENT_URL_VALUE)).thenReturn(document);

        documentSanitiser.sanitise(DOCUMENT_FIELD_TYPE, DOCUMENT_VALUE_INITIAL);
    }

    @Test(expected = ValidationException.class)
    public void shouldFailToSanitizeIfDocumentRetrievedButNullDocumentFilename() throws Exception {
        final Document document = buildDocument(BINARY_URL);
        document.setOriginalDocumentName(null);
        when(documentManagementRestClient.getDocument(DOCUMENT_FIELD_TYPE, DOCUMENT_URL_VALUE)).thenReturn(document);

        documentSanitiser.sanitise(DOCUMENT_FIELD_TYPE, DOCUMENT_VALUE_INITIAL);
    }

    private Document buildDocument() {
        Document document = new Document();
        _links links = new _links();
        Binary binary = new Binary();
        links.setBinary(binary);
        document.set_links(links);
        return document;
    }

    private Document buildDocument(final String href) {
        Document document = new Document();
        _links links = new _links();
        Binary binary = new Binary();
        binary.setHref(href);
        links.setBinary(binary);
        document.set_links(links);
        return document;
    }

}
