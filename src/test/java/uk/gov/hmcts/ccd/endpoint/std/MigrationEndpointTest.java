package uk.gov.hmcts.ccd.endpoint.std;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.migration.MigrationParameters;
import uk.gov.hmcts.ccd.domain.model.migration.MigrationResult;
import uk.gov.hmcts.ccd.domain.service.migration.CaseLinkMigrationService;
import uk.gov.hmcts.ccd.domain.service.search.SearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchQueryHelper;
import uk.gov.hmcts.ccd.endpoint.exceptions.ForbiddenException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static java.util.Collections.emptyList;

class MigrationEndpointTest {

    private MigrationEndpoint endpoint;

    @Mock
    private CaseLinkMigrationService service;
    @Mock
    private SearchOperation operation;
    @Mock
    private ElasticsearchQueryHelper elasticsearchQueryHelper;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);

        endpoint = new MigrationEndpoint(service, operation, elasticsearchQueryHelper);
    }

    @Test
    void shouldRunNoCasesFound() {
        String caseTypeId = "CaseTypeId";
        String jurisdiction = "Jurisdiction";
        Long caseDataId = 1L;
        Integer numRecords = 5;

        List<String> caseTypesAvailableToUser = new ArrayList<>();
        caseTypesAvailableToUser.add("CaseTypeId");

        MigrationParameters migrationParameters =
            new MigrationParameters(caseTypeId, jurisdiction, caseDataId, numRecords);

        doReturn(caseTypesAvailableToUser).when(elasticsearchQueryHelper).getCaseTypesAvailableToUser();
        doReturn(emptyList()).when(operation).execute(migrationParameters);

        MigrationResult result = endpoint.backPopulateCaseLinks(migrationParameters);

        assertEquals(0, result.getFinalRecordId());
        assertEquals(0, result.getRecordCount());

        verify(service, times(1)).backPopulateCaseLinkTable(any());

    }

    @Test
    void shouldNotAuthoriseCaseTypeId() {
        String caseTypeId = "CaseTypeId1";
        String jurisdiction = "Jurisdiction";
        Long caseDataId = 1L;
        Integer numRecords = 5;

        // GIVEN
        List<String> caseTypesAvailableToUser = new ArrayList<>();
        caseTypesAvailableToUser.add("CaseTypeId2");

        doReturn(caseTypesAvailableToUser).when(elasticsearchQueryHelper).getCaseTypesAvailableToUser();

        MigrationParameters migrationParameters =
            new MigrationParameters(caseTypeId, jurisdiction, caseDataId, numRecords);

        // WHEN
        Exception exception = assertThrows(ForbiddenException.class, () -> {
            endpoint.backPopulateCaseLinks(migrationParameters);
        });

        // THEN
        String expectedMessage = "Forbidden";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.equals(expectedMessage));
        verify(service, times(0)).backPopulateCaseLinkTable(any());
    }

    @Test
    void shouldRunSingleCaseFound() {
        String caseTypeId = "CaseTypeId";
        String jurisdiction = "Jurisdiction";
        Long caseDataId = 1L;
        Integer numRecords = 5;

        List<String> caseTypesAvailableToUser = new ArrayList<>();
        caseTypesAvailableToUser.add("CaseTypeId");

        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId("1");

        List<CaseDetails> cases = new ArrayList<>();
        cases.add(caseDetails);

        MigrationParameters migrationParameters =
            new MigrationParameters(caseTypeId, jurisdiction, caseDataId, numRecords);

        doReturn(caseTypesAvailableToUser).when(elasticsearchQueryHelper).getCaseTypesAvailableToUser();
        doReturn(cases).when(operation).execute(migrationParameters);

        MigrationResult result = endpoint.backPopulateCaseLinks(migrationParameters);

        assertEquals(1L, result.getFinalRecordId());
        assertEquals(1, result.getRecordCount());

        verify(service, times(1)).backPopulateCaseLinkTable(any());
    }

    @Test
    void shouldRunMultipleCasesFound() {
        String caseTypeId = "CaseTypeId";
        String jurisdiction = "Jurisdiction";
        Long caseDataId = 1L;
        Integer numRecords = 5;

        List<String> caseTypesAvailableToUser = new ArrayList<>();
        caseTypesAvailableToUser.add("CaseTypeId");

        CaseDetails caseDetails1 = new CaseDetails();
        caseDetails1.setId("1");
        CaseDetails caseDetails2 = new CaseDetails();
        caseDetails2.setId("2");

        List<CaseDetails> cases = new ArrayList<>();
        cases.add(caseDetails1);
        cases.add(caseDetails2);

        MigrationParameters migrationParameters =
            new MigrationParameters(caseTypeId, jurisdiction, caseDataId, numRecords);

        doReturn(caseTypesAvailableToUser).when(elasticsearchQueryHelper).getCaseTypesAvailableToUser();
        doReturn(cases).when(operation).execute(migrationParameters);

        MigrationResult result = endpoint.backPopulateCaseLinks(migrationParameters);

        assertEquals(2L, result.getFinalRecordId());
        assertEquals(2, result.getRecordCount());

        verify(service, times(1)).backPopulateCaseLinkTable(any());
    }

}
