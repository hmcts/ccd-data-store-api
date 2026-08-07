package uk.gov.hmcts.ccd.data.definition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.service.callbacks.CallbackUrlValidator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class DefaultCaseDefinitionRepositoryCoverageTest {

    @Mock
    private ApplicationParams applicationParams;
    @Mock
    private DefinitionStoreClient definitionStoreClient;
    @Mock
    private CallbackUrlValidator callbackUrlValidator;

    private DefaultCaseDefinitionRepository subject;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        subject = new DefaultCaseDefinitionRepository(applicationParams, definitionStoreClient, callbackUrlValidator);
        when(applicationParams.jurisdictionDefURL()).thenReturn("http://localhost/jurisdictions");
    }

    @Test
    void shouldReturnEmptyCaseTypeIdsWhenJurisdictionLookupIsEmpty() {
        when(definitionStoreClient.invokeGetRequest(anyString(), eq(JurisdictionDefinition[].class)))
            .thenReturn(new ResponseEntity<>(new JurisdictionDefinition[] {}, HttpStatus.OK));

        List<String> result = subject.getCaseTypesIDsByJurisdictions(List.of("J1"));

        assertEquals(List.of(), result);
    }

    @Test
    void shouldReturnDistinctCaseTypeIdsAcrossJurisdictions() {
        JurisdictionDefinition j1 = new JurisdictionDefinition();
        j1.setCaseTypeDefinitions(caseTypeDefinitions("A", "B"));
        JurisdictionDefinition j2 = new JurisdictionDefinition();
        j2.setCaseTypeDefinitions(caseTypeDefinitions("B", "C"));

        when(definitionStoreClient.invokeGetRequest(anyString(), eq(JurisdictionDefinition[].class)))
            .thenReturn(new ResponseEntity<>(new JurisdictionDefinition[] {j1, j2}, HttpStatus.OK));

        List<String> result = subject.getAllCaseTypesIDs();

        assertEquals(List.of("A", "B", "C"), result);
    }

    @Test
    void shouldReturnEmptyClassificationsWhenUserRoleListIsEmpty() {
        assertEquals(List.of(), subject.getClassificationsForUserRoleList(List.of()));
    }

    private List<CaseTypeDefinition> caseTypeDefinitions(String... ids) {
        return List.of(ids).stream().map(id -> {
            CaseTypeDefinition def = new CaseTypeDefinition();
            def.setId(id);
            return def;
        }).toList();
    }
}
