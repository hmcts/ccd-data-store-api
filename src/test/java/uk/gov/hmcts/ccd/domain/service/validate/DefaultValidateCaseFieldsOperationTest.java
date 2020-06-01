package uk.gov.hmcts.ccd.domain.service.validate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldComplexDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.Version;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.processor.FieldProcessorService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;

class DefaultValidateCaseFieldsOperationTest {

    private static final String CASE_TYPE_ID = "caseTypeId";
    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;
    @Mock
    private CaseTypeService caseTypeService;
    @Mock
    private FieldProcessorService fieldProcessorService;
    @Mock
    private CaseTypeDefinition caseTypeDefinition;
    @Mock
    private CaseDataContent caseDataContent;

    private String eventId = "eventId";
    private Map<String, JsonNode> data = Maps.newHashMap();

    private static final String JURISDICTION_ID = "jid";

    private ValidateCaseFieldsOperation validateCaseFieldsOperation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        doReturn(true).when(caseTypeDefinition).hasEventId(eventId);

        doReturn(data).when(caseDataContent).getData();
        Event event = anEvent().withEventId(eventId).build();
        doReturn(event).when(caseDataContent).getEvent();
        doReturn(eventId).when(caseDataContent).getEventId();

        validateCaseFieldsOperation = new DefaultValidateCaseFieldsOperation(caseDefinitionRepository, caseTypeService, fieldProcessorService);
    }

    @Test
    void shouldValidate_when_organisation_has_correct_role() throws Exception {

        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(buildCaseType("default_role"));

        final Map<String, JsonNode> organisationPolicyData = buildJsonNodeDataWithOrganisationPolicyRole("default_role");
        doReturn(organisationPolicyData).when(caseDataContent).getData();

        final Map<String, JsonNode> result = validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, caseDataContent);

        assertAll(
            () -> assertThat(result, is(organisationPolicyData))
        );
    }

    @Test
    void should_fail_validate_when_organisation_has_correct_role() throws Exception {

        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(buildCaseType("default_role"));

        final Map<String, JsonNode> organisationPolicyData = buildJsonNodeDataWithOrganisationPolicyRole("incorrect_role");
        doReturn(organisationPolicyData).when(caseDataContent).getData();

        ValidationException exception =
            assertThrows(ValidationException.class, () -> validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, caseDataContent));

        assertThat(exception.getMessage(),
            startsWith("The organisation policy role filed has an incorrect value."));

    }

    @Test
    void shouldValidateCaseDetails() {
        Map<String, JsonNode> result = validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, caseDataContent);

        assertAll(
            () -> verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
            () -> verify(caseTypeService).validateData(data, caseTypeDefinition),
            () -> assertThat(result, is(data))
        );
    }

    @Test
    void shouldFailValidationIfNoContent() {
        ValidationException exception =
            assertThrows(ValidationException.class, () -> validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, null));
        assertThat(exception.getMessage(),
            startsWith("Cannot validate case field because of event is not specified"));
        verify(caseDefinitionRepository, never()).getCaseType(any());
        verify(caseTypeService, never()).validateData(anyMap(), any());
    }

    @Test
    void shouldFailValidationIfNoEventInContent() {
        doReturn(null).when(caseDataContent).getEvent();

        ValidationException exception =
            assertThrows(ValidationException.class, () -> validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, caseDataContent));
        assertThat(exception.getMessage(),
            startsWith("Cannot validate case field because of event is not specified"));
        verify(caseDefinitionRepository, never()).getCaseType(any());
        verify(caseTypeService, never()).validateData(anyMap(), any());
    }

    @Test
    void shouldFailValidationIfNoEventIdInContent() {
        doReturn(null).when(caseDataContent).getEventId();

        ValidationException exception =
            assertThrows(ValidationException.class, () -> validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, caseDataContent));
        assertThat(exception.getMessage(),
            startsWith("Cannot validate case field because of event is not specified"));
        verify(caseDefinitionRepository, never()).getCaseType(any());
        verify(caseTypeService, never()).validateData(anyMap(), any());
    }

    @Test
    void shouldFailValidationIfNoCaseType() {
        doReturn(null).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);

        ValidationException exception =
            assertThrows(ValidationException.class, () -> validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, caseDataContent));
        assertThat(exception.getMessage(),
            startsWith("Cannot find case type definition for " + CASE_TYPE_ID));
        verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        verify(caseTypeService, never()).validateData(anyMap(), any());
    }

    @Test
    void shouldFailValidationIfEventIdNotFoundInCaseType() {
        doReturn("otherEvent").when(caseDataContent).getEventId();

        ValidationException exception =
            assertThrows(ValidationException.class, () -> validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, caseDataContent));
        assertThat(exception.getMessage(),
            startsWith("Cannot validate case field because of event otherEvent is not found in case type definition"));
        verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        verify(caseTypeService, never()).validateData(anyMap(), any());
    }

    private Map<String, JsonNode> buildJsonNodeDataWithOrganisationPolicyRole(String organisationPolicyRole) throws IOException {
        final JsonNode node = new ObjectMapper().readTree("{\n"
            + "  \"OrgPolicyCaseAssignedRole\": \"" + organisationPolicyRole + "\",\n"
            + "  \"OrgPolicyReference\": \"" + organisationPolicyRole + "\",\n"
            + "  \"Organisation\": {\n"
            + "    \"OrganisationID\": \"OrganisationID\",\n"
            + "    \"OrganisationName\": " + "\"OrganisationName\"\n"
            + "  }\n"
            + "}\n");
        final Map<String, JsonNode> map = new HashMap<>();
        map.put("test", node);
        return map;
    }

    private static CaseTypeDefinition buildCaseType(String defaultValue) {
        final List<CaseEventDefinition> CaseEventDefinitions = new ArrayList();
        final List<CaseEventFieldDefinition> caseEventFieldDefinitions = new ArrayList<>();
        final List<CaseEventFieldComplexDefinition> caseEventFieldComplexDefinitions = new ArrayList<>();
        final JurisdictionDefinition j = buildJurisdiction();
        final Version version = new Version();
        version.setNumber(67);
        final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        final CaseEventFieldDefinition caseEventFieldDefinition = new CaseEventFieldDefinition();
        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        final CaseEventFieldComplexDefinition caseEventFieldComplexDefinition = new CaseEventFieldComplexDefinition();
        caseEventFieldComplexDefinition.setDefaultValue(defaultValue);
        caseEventFieldComplexDefinition.setReference(DefaultValidateCaseFieldsOperation.ORGANISATION_POLICY_ROLE);

        caseEventFieldComplexDefinitions.add(caseEventFieldComplexDefinition);
        caseEventFieldDefinitions.add(caseEventFieldDefinition);
        caseEventFieldDefinition.setCaseEventFieldComplexDefinitions(caseEventFieldComplexDefinitions);

        caseEventDefinition.setId("eventId");
        caseEventDefinition.setCaseFields(caseEventFieldDefinitions);

        caseTypeDefinition.setId("caseTypeId");
        caseTypeDefinition.setName("case type name");
        caseTypeDefinition.setJurisdictionDefinition(j);
        caseTypeDefinition.setVersion(version);
        CaseEventDefinitions.add(caseEventDefinition);
        caseTypeDefinition.setEvents(CaseEventDefinitions);
        return caseTypeDefinition;
    }

    private static JurisdictionDefinition buildJurisdiction() {
        final JurisdictionDefinition j = new JurisdictionDefinition();
        j.setId(JURISDICTION_ID);
        return j;
    }
}
