package uk.gov.hmcts.ccd.v2.external.resource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.Link;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentPermissions;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocumentMetadata;
import uk.gov.hmcts.ccd.v2.external.domain.Permission;

import java.util.Arrays;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("CaseDocumentResource")
class DocumentPermissionsResourceTest {
    private static final String CASE_REFERENCE = "1234123412341238";
    private static final String CASE_TYPE_ID = "BEFTA_CASETYPE_2_1";
    private static final String JURISDICTION_ID = "BEFTA_JURISDICTION_2";
    private static final String CASE_DOCUMENT_ID = "a780ee98-3136-4be9-bf56-a46f8da1bc97";
    private static final String DOCUMENT_URL = "http://dm-store:8080/documents/a780ee98-3136-4be9-bf56-a46f8da1bc97";
    private static final String DOCUMENT_NAME = "Sample_document.txt";
    private static final String DOCUMENT_TYPE = "Document";

    private final String linkSelfForCaseDocument = String.format("/cases/%s/documents/%s", CASE_REFERENCE, CASE_DOCUMENT_ID);
    private final CaseDocumentMetadata caseDocumentMetadata = CaseDocumentMetadata.builder()
        .caseId(CASE_REFERENCE)
            .caseTypeId(CASE_TYPE_ID)
            .jurisdictionId(JURISDICTION_ID)
            .document(DocumentPermissions.builder()
                .id(CASE_DOCUMENT_ID)
                .url(DOCUMENT_URL)
                .name(DOCUMENT_NAME)
                .type(DOCUMENT_TYPE)
                .permissions(Arrays.asList(Permission.READ, Permission.UPDATE))
        .build())
        .build();

    @Test
    @DisplayName("should copy case document metadata unwrapped")
    void shouldCopyUnwrappedCaseDocumentMetadataContent() {
        final CaseDocumentResource result = new CaseDocumentResource(CASE_REFERENCE, CASE_DOCUMENT_ID, caseDocumentMetadata);

        assertAll(
            () -> assertThat(result.getDocumentMetadata(), equalTo(caseDocumentMetadata))
        );
    }

    @Test
    @DisplayName("should link to itself")
    void shouldLinkToSelf() {
        final CaseDocumentResource result = new CaseDocumentResource(CASE_REFERENCE, CASE_DOCUMENT_ID, caseDocumentMetadata);

        Optional<Link> self = (Optional<Link>) result.getLink("self");
        if (self.isPresent()) {
            assertThat(self.get().getHref(), equalTo(linkSelfForCaseDocument));
        }
    }

}
