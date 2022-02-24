package uk.gov.hmcts.ccd.domain.service.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.caseaccess.CaseLinkRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedeletion.CaseLinkExtractor;
import uk.gov.hmcts.ccd.domain.service.casedeletion.CaseLinkService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CaseLinkMigrationServiceTest {

    private static final String JURISDICTION_ID = "jurisdictionId";
    private static final String CASE_TYPE_ID = "caseTypeId";
    private static final Long CASE_REFERENCE = 12356878998658L;

    private static final String CASE_LINK_REFERENCE_1 = "12356878998659";
    private static final String CASE_LINK_REFERENCE_2 = "12356878998670";

    @InjectMocks
    private CaseLinkMigrationService caseLinkMigrationService;

    @Mock
    private CaseLinkRepository caseLinkRepository;

    @Mock
    private CaseLinkService caseLinkService;

    @Mock
    private CaseLinkExtractor caseLinkExtractor;

    @Mock
    private CaseTypeDefinition caseTypeDefinition;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    private final Map<String, JsonNode> data = new HashMap<>();

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        caseLinkMigrationService
            = new CaseLinkMigrationService(caseLinkService, caseLinkExtractor, caseDefinitionRepository);

        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(caseTypeDefinition);
    }

    @Test
    public void noCasesToBackPopulate() {
        List<CaseDetails> cases = new ArrayList<>();

        caseLinkMigrationService.backPopulateCaseLinkTable(cases);

        verify(caseLinkService, times(0)).updateCaseLinks(any(), any(), anyList());
    }

    @Test
    public void backPopulateMissingCaseLinks() {
        List<CaseDetails> cases = new ArrayList<>();
        cases.add(createNewCaseDetails(CASE_TYPE_ID, JURISDICTION_ID, data));

        List<String> caseLinks = createCaseLinks();

        caseLinkMigrationService.backPopulateCaseLinkTable(cases);

        verify(caseLinkService, times(1)).updateCaseLinks(CASE_REFERENCE, CASE_TYPE_ID, caseLinks);
    }


    @Test
    public void backPopulateSingleCase() {
        List<CaseDetails> cases = new ArrayList<>();
        cases.add(createNewCaseDetails(CASE_TYPE_ID, JURISDICTION_ID, data));

        List<String> caseLinks = createCaseLinks(CASE_LINK_REFERENCE_1);

        when(caseLinkExtractor.getCaseLinks(data, caseTypeDefinition.getCaseFieldDefinitions())).thenReturn(caseLinks);

        caseLinkMigrationService.backPopulateCaseLinkTable(cases);

        verify(caseLinkService, times(1)).updateCaseLinks(CASE_REFERENCE, CASE_TYPE_ID, caseLinks);
    }

    @Test
    public void backPopulateMultipleCases() {
        List<CaseDetails> cases = new ArrayList<>();
        cases.add(createNewCaseDetails(CASE_TYPE_ID, JURISDICTION_ID, data));
        cases.add(createNewCaseDetails(CASE_TYPE_ID, JURISDICTION_ID, data));

        List<String> caseLinks = createCaseLinks(CASE_LINK_REFERENCE_1, CASE_LINK_REFERENCE_2);

        when(caseLinkExtractor.getCaseLinks(data, caseTypeDefinition.getCaseFieldDefinitions())).thenReturn(caseLinks);

        caseLinkMigrationService.backPopulateCaseLinkTable(cases);

        verify(caseLinkService, times(2)).updateCaseLinks(CASE_REFERENCE, CASE_TYPE_ID, caseLinks);
    }

    private CaseDetails createNewCaseDetails(String caseTypeId, String jurisdictionId, Map<String, JsonNode> data) {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setReference(CASE_REFERENCE);
        caseDetails.setCaseTypeId(caseTypeId);
        caseDetails.setJurisdiction(jurisdictionId);
        caseDetails.setData(data == null ? Maps.newHashMap() : data);
        return caseDetails;
    }

    private List<String> createCaseLinks(String ... caseLinkReferences) {
        return new ArrayList<>(Arrays.asList(caseLinkReferences));
    }
}
