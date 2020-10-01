package uk.gov.hmcts.ccd.domain.service.stdapi;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.Document;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrintableDocumentListOperationTest {
    private static final String JURISDICTION_ID = "TEST_JURISDICTION";
    private static final String CASE_TYPE_ID = "TEST_CASE_TYPE";
    private static final Long CASE_REFERENCE = 1111222233334444L;
    private static final String PRINT_URL =
        "http://localhost:9999/jurisdictions/:jid/case-types/:ctid/cases/:cid/documents";
    private static final String EXPECTED_PRINT_URL =
        "http://localhost:9999/jurisdictions/TEST_JURISDICTION/case-types/TEST_CASE_TYPE/cases/1111222233334444/documents";
    private static final String PRINT_URL_WITH_OMITTED_PARAM =
        "http://localhost:9999/jurisdictions/case-types/:ctid/cases/:cid/documents";
    private static final String EXPECTED_PRINT_URL_WITH_OMITTED_PARAM =
        "http://localhost:9999/jurisdictions/case-types/TEST_CASE_TYPE/cases/1111222233334444/documents";
    private static final String PRINT_URL_WITH_UNKNOWN_PARAM =
        "http://localhost:9999/jurisdictions/:xxx/case-types/:ctid/cases/:cid/documents";
    private static final String EXPECTED_PRINT_URL_WITH_UNKNOWN_PARAM =
        "http://localhost:9999/jurisdictions/:xxx/case-types/TEST_CASE_TYPE/cases/1111222233334444/documents";
    private static final String DOCUMENT_NAME = "TEST_DOCUMENT";
    private static final String DOCUMENT_DESCRIPTION = "A test document";
    private static final String DOCUMENT_TYPE = "Default type";
    private static final String OTHER_PRINT_HOSTNAME = "http://otherlocalhost";
    private static final String OTHER_DOCUMENT_NAME = "Other_TEST_DOCUMENT";
    private static final String OTHER_DOCUMENT_DESCRIPTION = "Another test document";
    private static final String OTHER_DOCUMENT_TYPE = "Other type";

    private PrintableDocumentListOperation documentOperation;
    private ApplicationParams applicationParams;
    private CaseDetails caseDetails;

    @BeforeEach
    void setUp() {
        applicationParams = mock(ApplicationParams.class);
        when(applicationParams.getDefaultPrintUrl()).thenReturn(PRINT_URL);
        when(applicationParams.getDefaultPrintName()).thenReturn(DOCUMENT_NAME);
        when(applicationParams.getDefaultPrintDescription()).thenReturn(DOCUMENT_DESCRIPTION);
        when(applicationParams.getDefaultPrintType()).thenReturn(DOCUMENT_TYPE);

        documentOperation = new PrintableDocumentListOperation(applicationParams);

        caseDetails = new CaseDetails();
        caseDetails.setReference(CASE_REFERENCE);
    }

    @Test
    @DisplayName("should return a list of printable Documents, containing one Document")
    void shouldReturnPrintableDocumentList() {
        final List<Document> printableDocumentList = documentOperation.getPrintableDocumentList(JURISDICTION_ID,
            CASE_TYPE_ID, caseDetails);

        assertAll(
            () -> assertThat(printableDocumentList, hasSize(1)),
            () -> assertThat(printableDocumentList, hasItem(
                allOf(
                    hasProperty("name", equalTo(DOCUMENT_NAME)),
                    hasProperty("description", equalTo(DOCUMENT_DESCRIPTION)),
                    hasProperty("type", equalTo(DOCUMENT_TYPE))
                )
            ))
        );
    }

    @Test
    @DisplayName("should build Printable Document URL according to pattern from configuration")
    void shouldBuildPrintUrlFromConfig() {
        final List<Document> printableDocumentList = documentOperation.getPrintableDocumentList(JURISDICTION_ID,
            CASE_TYPE_ID, caseDetails);

        assertAll(
            () -> assertThat(printableDocumentList, hasItem(
                hasProperty("url", equalTo(EXPECTED_PRINT_URL))
            ))
        );
    }

    @Test
    @DisplayName("should build Printable Document URL even if a template parameter has been omitted")
    void shouldBuildPrintUrlFromConfigWithOmittedParam() {
        when(applicationParams.getDefaultPrintUrl()).thenReturn(PRINT_URL_WITH_OMITTED_PARAM);

        final List<Document> printableDocumentList = documentOperation.getPrintableDocumentList(JURISDICTION_ID,
            CASE_TYPE_ID, caseDetails);

        assertAll(
            () -> assertThat(printableDocumentList, hasItem(
                hasProperty("url", equalTo(EXPECTED_PRINT_URL_WITH_OMITTED_PARAM))
            ))
        );
    }

    @Test
    @DisplayName("should build Printable Document URL even if a template parameter is unknown")
    void shouldBuildPrintUrlFromConfigWithUnknownParam() {
        when(applicationParams.getDefaultPrintUrl()).thenReturn(PRINT_URL_WITH_UNKNOWN_PARAM);

        final List<Document> printableDocumentList = documentOperation.getPrintableDocumentList(JURISDICTION_ID,
            CASE_TYPE_ID, caseDetails);

        assertAll(
            () -> assertThat(printableDocumentList, hasItem(
                hasProperty("url", equalTo(EXPECTED_PRINT_URL_WITH_UNKNOWN_PARAM))
            ))
        );
    }

    @Test
    @DisplayName("should throw an IllegalArgumentException if the Jurisdiction ID is null")
    void shouldThrowExceptionIfJurisdictionIdIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            documentOperation.getPrintableDocumentList(null, CASE_TYPE_ID, caseDetails));
    }

    @Test
    @DisplayName("should throw an IllegalArgumentException if the Case Type ID is null")
    void shouldThrowExceptionIfCaseTypeIdIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            documentOperation.getPrintableDocumentList(JURISDICTION_ID, null, caseDetails));
    }

    @Test
    @DisplayName("should throw an IllegalArgumentException if the Case Reference is null")
    void shouldThrowExceptionIfCaseReferenceIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            documentOperation.getPrintableDocumentList(JURISDICTION_ID, CASE_TYPE_ID, null));
    }

    @Test
    @DisplayName("should throw an IllegalArgumentException if the Jurisdiction ID is whitespace")
    void shouldThrowExceptionIfJurisdictionIdIsWhitespace() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () ->
                documentOperation.getPrintableDocumentList("", CASE_TYPE_ID, caseDetails)),
            () -> assertThrows(IllegalArgumentException.class, () ->
                documentOperation.getPrintableDocumentList(" ", CASE_TYPE_ID, caseDetails))
        );
    }

    @Test
    @DisplayName("should throw an IllegalArgumentException if the Case Type ID is whitespace")
    void shouldThrowExceptionIfCaseTypeIdIsWhitespace() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () ->
                documentOperation.getPrintableDocumentList(JURISDICTION_ID, "", caseDetails)),
            () -> assertThrows(IllegalArgumentException.class, () ->
                documentOperation.getPrintableDocumentList(JURISDICTION_ID, " ", caseDetails))
        );
    }

    @Test
    @DisplayName("should throw an IllegalArgumentException if the Case Reference is whitespace")
    void shouldThrowExceptionIfCaseReferenceIsWhitespace() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () ->
                documentOperation.getPrintableDocumentList(JURISDICTION_ID, CASE_TYPE_ID, null)),
            () -> assertThrows(IllegalArgumentException.class, () ->
                documentOperation.getPrintableDocumentList(JURISDICTION_ID, CASE_TYPE_ID, new CaseDetails()))
        );
    }

    @Test
    @DisplayName("should get Document metadata from configuration")
    void shouldGetDocumentMetadataFromConfig() {
        when(applicationParams.getDefaultPrintUrl()).thenReturn(OTHER_PRINT_HOSTNAME);
        when(applicationParams.getDefaultPrintName()).thenReturn(OTHER_DOCUMENT_NAME);
        when(applicationParams.getDefaultPrintDescription()).thenReturn(OTHER_DOCUMENT_DESCRIPTION);
        when(applicationParams.getDefaultPrintType()).thenReturn(OTHER_DOCUMENT_TYPE);

        final List<Document> printableDocumentList = documentOperation.getPrintableDocumentList(JURISDICTION_ID,
            CASE_TYPE_ID, caseDetails);

        assertAll(
            () -> assertThat(printableDocumentList, hasSize(1)),
            () -> assertThat(printableDocumentList, hasItem(
                allOf(
                    hasProperty("url", Matchers.startsWith(OTHER_PRINT_HOSTNAME)),
                    hasProperty("name", equalTo(OTHER_DOCUMENT_NAME)),
                    hasProperty("description", equalTo(OTHER_DOCUMENT_DESCRIPTION)),
                    hasProperty("type", equalTo(OTHER_DOCUMENT_TYPE))
                )
            ))
        );
    }
}
