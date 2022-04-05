package uk.gov.hmcts.ccd.domain.service.migration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.casedeletion.CaseLink;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedeletion.CaseLinkExtractor;
import uk.gov.hmcts.ccd.domain.service.casedeletion.CaseLinkService;
import uk.gov.hmcts.ccd.domain.service.casedeletion.CaseLinkTestFixtures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CaseLinkMigrationServiceTest extends CaseLinkTestFixtures {

    @Mock
    private CaseLinkService caseLinkService;

    @Mock
    private CaseLinkExtractor caseLinkExtractor;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @InjectMocks
    private CaseLinkMigrationService caseLinkMigrationService;

    private final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();

    private final Map<String, JsonNode> data = new HashMap<>();

    @Test
    void noCasesToBackPopulate() {

        // GIVEN
        List<CaseDetails> cases = new ArrayList<>();

        // WHEN
        caseLinkMigrationService.backPopulateCaseLinkTable(cases);

        // THEN
        verifyNoInteractions(caseLinkExtractor);
        verifyNoInteractions(caseLinkService);
    }


    @Test
    void backPopulateSingleCase() {

        // GIVEN
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(caseTypeDefinition);

        List<CaseDetails> cases = new ArrayList<>();
        CaseDetails caseDetails = createCaseDetails(data);
        cases.add(caseDetails);

        List<CaseLink> caseLinks = createCaseLinks(LINKED_CASE_REFERENCE_01);

        given(caseLinkExtractor.getCaseLinksFromData(caseDetails, caseTypeDefinition.getCaseFieldDefinitions()))
            .willReturn(caseLinks);

        // WHEN
        caseLinkMigrationService.backPopulateCaseLinkTable(cases);

        // THEN
        verify(caseLinkExtractor,
            times(1)).getCaseLinksFromData(eq(caseDetails), anyList());
        verify(caseLinkService,
            times(1)).updateCaseLinks(CASE_REFERENCE, CASE_TYPE_ID, caseLinks);
    }

    @Test
    void backPopulateMultipleCases() {

        // GIVEN
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(caseTypeDefinition);
        CaseTypeDefinition caseTypeDefinition2 = new CaseTypeDefinition();
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID_02)).willReturn(caseTypeDefinition2);

        List<CaseDetails> cases = new ArrayList<>();
        CaseDetails caseDetails1 = createCaseDetails(CASE_REFERENCE, CASE_TYPE_ID, data);
        CaseDetails caseDetails2 = createCaseDetails(CASE_REFERENCE_02, CASE_TYPE_ID_02, data);
        cases.add(caseDetails1);
        cases.add(caseDetails2);

        List<CaseLink> caseLinks1 = createCaseLinks(LINKED_CASE_REFERENCE_01, LINKED_CASE_REFERENCE_02);
        List<CaseLink> caseLinks2 = createCaseLinks(LINKED_CASE_REFERENCE_02, LINKED_CASE_REFERENCE_03);

        given(caseLinkExtractor.getCaseLinksFromData(caseDetails1, caseTypeDefinition.getCaseFieldDefinitions()))
            .willReturn(caseLinks1);
        given(caseLinkExtractor.getCaseLinksFromData(caseDetails2, caseTypeDefinition2.getCaseFieldDefinitions()))
            .willReturn(caseLinks2);

        // WHEN
        caseLinkMigrationService.backPopulateCaseLinkTable(cases);

        // THEN
        verify(caseLinkExtractor,
            times(2)).getCaseLinksFromData(any(CaseDetails.class), anyList());
        verify(caseLinkExtractor,
            times(1)).getCaseLinksFromData(eq(caseDetails1), anyList());
        verify(caseLinkExtractor,
            times(1)).getCaseLinksFromData(eq(caseDetails2), anyList());

        verify(caseLinkService,
            times(2)).updateCaseLinks(anyLong(), anyString(), anyList());
        verify(caseLinkService).updateCaseLinks(CASE_REFERENCE, CASE_TYPE_ID, caseLinks1);
        verify(caseLinkService).updateCaseLinks(CASE_REFERENCE_02, CASE_TYPE_ID_02, caseLinks2);
    }

    private List<CaseLink> createCaseLinks(Long... caseLinkReferences) {
        return Arrays.stream(caseLinkReferences)
            .map(caseLinkReference -> CaseLink.builder()
                .linkedCaseReference(caseLinkReference)
                .standardLink(false).build())
            .collect(Collectors.toList());
    }
}
