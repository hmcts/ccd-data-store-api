package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.search.CaseDocumentsMetadata;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CaseDocumentMetadataMapperTest extends WireMockBaseTest {

    @Autowired
    private CaseDocumentMetadataMapper mapper;

    private static final String CASE_ID = "CaseId";
    private static final String CASE_TYPE_ID = "CaseTypeId";
    private static final String JURISDICTION_ID = "JurisdictionId";
    private static final String ID_1 = "1";
    private static final String HASH_TOKEN_1 = "ht1";
    private static final String ID_2 = "2";
    private static final String HASH_TOKEN_2 = "ht2";


    private static final List<DocumentHashToken> DOCUMENT_HASH_TOKENS = Stream.of(
        DocumentHashToken.builder()
            .id(ID_1)
            .hashToken(HASH_TOKEN_1)
            .build(),
        DocumentHashToken.builder()
            .id(ID_2)
            .hashToken(HASH_TOKEN_2)
            .build()
    ).collect(Collectors.toList());

    @Test
    void successfulMapping() {
        // GIVEN
        CaseDocumentsMetadata ccdCaseDocumentsMetadata = CaseDocumentsMetadata.builder()
            .caseId(CASE_ID)
            .caseTypeId(CASE_TYPE_ID)
            .jurisdictionId(JURISDICTION_ID)
            .documentHashTokens(DOCUMENT_HASH_TOKENS)
            .build();

        // WHEN
        uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentsMetadata caseDocumentsMetadata
            = mapper.convertToAmClientCaseDocumentsMetadata(ccdCaseDocumentsMetadata);

        // THEN
        assertAll(
            () -> assertEquals(ccdCaseDocumentsMetadata.getCaseId(), caseDocumentsMetadata.getCaseId()),
            () -> assertEquals(ccdCaseDocumentsMetadata.getCaseTypeId(), caseDocumentsMetadata.getCaseTypeId()),
            () -> assertEquals(ccdCaseDocumentsMetadata.getJurisdictionId(), caseDocumentsMetadata.getJurisdictionId()),
            () -> assertTrue(ccdCaseDocumentsMetadata.getDocumentHashTokens().stream()
                    .allMatch(this::containsAllExpectedDocumentHasTokenValues))
        );
    }

    private boolean containsAllExpectedDocumentHasTokenValues(DocumentHashToken documentHashToken) {
        return documentHashToken.getId().equals(ID_1) && documentHashToken.getHashToken().equals(HASH_TOKEN_1)
            ||
            documentHashToken.getId().equals(ID_2) && documentHashToken.getHashToken().equals(HASH_TOKEN_2);
    }
}
