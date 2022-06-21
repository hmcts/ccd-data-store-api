package uk.gov.hmcts.ccd.domain.service.caselinking;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.caselinking.CaseLinkService;
import uk.gov.hmcts.ccd.domain.service.caselinking.CaseLinkTestFixtures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CaseLinkMigrationServiceTest extends CaseLinkTestFixtures {

    @Mock
    private CaseLinkService caseLinkService;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @InjectMocks
    private CaseLinkMigrationService caseLinkMigrationService;

    private final CaseTypeDefinition caseTypeDefinition = createCaseTypeDefinition();

    private final Map<String, JsonNode> data = new HashMap<>();

    @Test
    void noCasesToBackPopulate() {

        // GIVEN
        List<CaseDetails> cases = new ArrayList<>();

        // WHEN
        caseLinkMigrationService.backPopulateCaseLinkTable(cases);

        // THEN
        verifyNoInteractions(caseLinkService);
    }

    @Test
    void backPopulateSingleCase() {

        // GIVEN
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(caseTypeDefinition);

        List<CaseDetails> cases = new ArrayList<>();
        CaseDetails caseDetails = createCaseDetails(data);
        cases.add(caseDetails);

        // WHEN
        caseLinkMigrationService.backPopulateCaseLinkTable(cases);

        // THEN
        verify(caseLinkService,
            times(1)).updateCaseLinks(caseDetails, caseTypeDefinition.getCaseFieldDefinitions());
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


        // WHEN
        caseLinkMigrationService.backPopulateCaseLinkTable(cases);

        // THEN
        verify(caseLinkService,
            times(2)).updateCaseLinks(any(CaseDetails.class), anyList());
        verify(caseLinkService).updateCaseLinks(caseDetails1, caseTypeDefinition.getCaseFieldDefinitions());
        verify(caseLinkService).updateCaseLinks(caseDetails2,  caseTypeDefinition2.getCaseFieldDefinitions());
    }

}
