package uk.gov.hmcts.ccd.domain.service.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;

class DefaultAuthorisedCaseDataFilterTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private SecurityClassificationService classificationService;
    @Mock
    private ObjectMapperService objectMapperService;

    @InjectMocks
    private DefaultAuthorisedCaseDataFilter authorisedCaseDataFilter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @DisplayName("should filter fields within case data")
    void shouldFilterFields() {
        Map<String, JsonNode> unFilteredData = new HashMap<>();
        Map<String, JsonNode> filteredData = new HashMap<>();
        CaseType caseType = new CaseType();
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setData(unFilteredData);
        JsonNode jsonNode = mock(JsonNode.class);
        Set<String> userRoles = new HashSet<>();
        when(userRepository.getUserRoles()).thenReturn(userRoles);
        when(objectMapperService.convertObjectToJsonNode(unFilteredData)).thenReturn(jsonNode);
        when(accessControlService.filterCaseFieldsByAccess(jsonNode, caseType.getCaseFields(), userRoles, CAN_READ)).thenReturn(jsonNode);
        when(objectMapperService.convertJsonNodeToMap(jsonNode)).thenReturn(filteredData);

        authorisedCaseDataFilter.filterFields(caseType, caseDetails);

        assertAll(
            () -> assertThat(caseDetails.getData(), is(filteredData)),
            () -> verify(userRepository).getUserRoles(),
            () -> verify(objectMapperService).convertObjectToJsonNode(unFilteredData),
            () -> verify(accessControlService).filterCaseFieldsByAccess(jsonNode, caseType.getCaseFields(), userRoles, CAN_READ),
            () -> verify(objectMapperService).convertJsonNodeToMap(jsonNode),
            () -> verify(classificationService).applyClassification(caseDetails)
        );
    }
}
