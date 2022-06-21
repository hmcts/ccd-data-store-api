package uk.gov.hmcts.ccd.v2.external.resource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.Link;

import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentPermissions;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocumentMetadata;
import uk.gov.hmcts.ccd.v2.external.domain.Permission;

import java.util.Arrays;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("CaseDocumentResource")
class CaseDocumentResourceTest {
    private static final String CASE_REFERENCE = "1234123412341238";
    private static final String CASE_DOCUMENT_ID = "a780ee98-3136-4be9-bf56-a46f8da1bc97";

    private final String linkSelfForCaseDocument
        = String.format("/cases/%s/documents/%s", CASE_REFERENCE, CASE_DOCUMENT_ID);
    private final CaseDocumentMetadata caseDocumentMetadata = CaseDocumentMetadata.builder()
        .caseId(CASE_REFERENCE)
        .documentPermissions(DocumentPermissions.builder()
            .id(CASE_DOCUMENT_ID)
            .permissions(Arrays.asList(Permission.READ, Permission.UPDATE))
            .build())
        .build();

    @Test
    @DisplayName("should copy case document metadata unwrapped")
    void shouldCopyUnwrappedCaseDocumentMetadataContent() {
        final CaseDocumentResource result
            = new CaseDocumentResource(CASE_REFERENCE, CASE_DOCUMENT_ID, caseDocumentMetadata);

        assertAll(() -> assertThat(result.getDocumentMetadata(), equalTo(caseDocumentMetadata)));
    }

    @Test
    @DisplayName("should throw Null Pointer Exception  when case id is null")
    void shouldThrowNullPointerExceptionWhenCaseIdNull() {
        assertThrows(NullPointerException.class,
            () -> new CaseDocumentResource(null, CASE_DOCUMENT_ID, caseDocumentMetadata));
    }

    @Test
    @DisplayName("should throw Null Pointer Exception  when document id is null")
    void shouldThrowNullPointerExceptionWhenDocumentIdNull() {
        assertThrows(NullPointerException.class,
            () -> new CaseDocumentResource(CASE_REFERENCE, null, caseDocumentMetadata));
    }

    @Test
    @DisplayName("should link to itself")
    void shouldLinkToSelf() {
        final CaseDocumentResource result
            = new CaseDocumentResource(CASE_REFERENCE, CASE_DOCUMENT_ID, caseDocumentMetadata);

        final Optional<Link> self = result.getLink("self");
        self.ifPresent(link -> assertThat(link.getHref(), endsWith(linkSelfForCaseDocument)));
    }

}
