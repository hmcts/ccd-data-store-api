package uk.gov.hmcts.ccd.domain.service.validate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.caseaccess.CaseRoleRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldComplexDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.Version;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.CaseFieldValidationError;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.processor.FieldProcessorService;
import uk.gov.hmcts.ccd.domain.types.ValidationContext;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseValidationException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.types.PredefinedFieldsIDs.ORG_POLICY_CASE_ASSIGNED_ROLE;

class DefaultValidateCaseFieldsOperationTest {


    private static final String ORGANISATION_POLICY_ROLE = ORG_POLICY_CASE_ASSIGNED_ROLE.getId();
    private static final String CASE_TYPE_ID = "caseTypeId";
    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;
    @Mock
    private CaseRoleRepository caseRoleRepository;
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
    private static final String ORGANISATIONPOLICYFIELD_1 = "OrganisationPolicyField1";
    private static final String ORGANISATIONPOLICYFIELD_2 = "OrganisationPolicyField2";

    private ValidateCaseFieldsOperation validateCaseFieldsOperation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        doReturn(Stream.of("[DEFAULT_ROLE1]", "[DEFAULT_ROLE2]").collect(Collectors.toSet()))
            .when(caseRoleRepository).getCaseRoles(CASE_TYPE_ID);
        doReturn(true).when(caseTypeDefinition).hasEventId(eventId);

        doReturn(data).when(caseDataContent).getData();
        Event event = anEvent().withEventId(eventId).build();
        doReturn(event).when(caseDataContent).getEvent();
        doReturn(eventId).when(caseDataContent).getEventId();

        validateCaseFieldsOperation = new DefaultValidateCaseFieldsOperation(caseDefinitionRepository,
            caseTypeService,
            fieldProcessorService);
    }

    @Test
    void shouldValidate_when_organisation_has_correct_role() throws Exception {

        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(buildCaseType());

        final Map<String, JsonNode> organisationPolicyData =
            buildJsonNodeDataWithOrganisationPolicyRole("[default_role1]");
        doReturn(organisationPolicyData).when(caseDataContent).getData();

        final Map<String, JsonNode> result =
            validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, caseDataContent);

        assertAll(
            () -> assertThat(result, is(organisationPolicyData))
        );
    }

    @Test
    void shouldValidate_when_organisation_has_correct_roles() throws Exception {

        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID))
            .willReturn(buildCaseTypeWithTwoDefaultValues("[default_role1]", "[default_role2]"));

        final Map<String, JsonNode> organisationPolicyData =
            buildJsonNodeDataWithTwoOrganisationPolicyRole("[default_role1]", "[default_role2]");
        doReturn(organisationPolicyData).when(caseDataContent).getData();

        final Map<String, JsonNode> result =
            validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, caseDataContent);

        assertAll(
            () -> assertThat(result, is(organisationPolicyData))
        );
    }

    @Test
    void shouldValidateData_when_organisation_has_correct_roles() throws Exception {
        CaseTypeDefinition caseTypeDefinition = buildCaseTypeWithTwoDefaultValues("[default_role1]", "[default_role2]");
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(caseTypeDefinition);

        final Map<String, JsonNode> organisationPolicyData =
            buildJsonNodeDataWithTwoOrganisationPolicyRole("[default_role1]", "[default_role2]");
        doReturn(organisationPolicyData).when(caseDataContent).getData();

        validateCaseFieldsOperation.validateData(organisationPolicyData, caseTypeDefinition, caseDataContent);
    }

    @Test
    void shouldValidateData_when_organisation_has_correct_roles_complex() throws Exception {

        String orgPolicyReference1 = ORGANISATIONPOLICYFIELD_1 + "." + ORGANISATION_POLICY_ROLE;
        String orgPolicyReference2 = ORGANISATIONPOLICYFIELD_2 + "." + ORGANISATION_POLICY_ROLE;
        CaseTypeDefinition caseTypeDefinition = buildCaseTypeWithTwoDefaultValues(
            "[default_role1]",
            "[default_role2]",
            orgPolicyReference1,
            orgPolicyReference2,
            "Class",
            "Class");
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(caseTypeDefinition);

        final Map<String, JsonNode> organisationPolicyData =
            buildJsonNodeDataWithTwoOrganisationPolicyRole("[default_role1]", "[default_role2]");

        ObjectNode classNode = new ObjectMapper().createObjectNode();
        organisationPolicyData.keySet().forEach(key -> classNode.set(key, organisationPolicyData.get(key)));

        ObjectNode parentNode = new ObjectMapper().createObjectNode();
        parentNode.set("Class", classNode);
        Map<String, JsonNode> parentData = new HashMap<>();
        parentData.put("ParentNode", parentNode);
        doReturn(parentData).when(caseDataContent).getData();

        validateCaseFieldsOperation.validateData(organisationPolicyData, caseTypeDefinition, caseDataContent);
    }

    @Test
    void shouldValidate_when_organisation_has_correct_roles_complex() throws Exception {

        String orgPolicyReference1 = ORGANISATIONPOLICYFIELD_1 + "." + ORGANISATION_POLICY_ROLE;
        String orgPolicyReference2 = ORGANISATIONPOLICYFIELD_2 + "." + ORGANISATION_POLICY_ROLE;
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(buildCaseTypeWithTwoDefaultValues(
            "[default_role1]",
            "[default_role2]",
            orgPolicyReference1,
            orgPolicyReference2,
            "Class",
            "Class"));

        final Map<String, JsonNode> organisationPolicyData =
            buildJsonNodeDataWithTwoOrganisationPolicyRole("[default_role1]", "[default_role2]");

        ObjectNode classNode = new ObjectMapper().createObjectNode();
        organisationPolicyData.keySet().forEach(key -> classNode.set(key, organisationPolicyData.get(key)));

        ObjectNode parentNode = new ObjectMapper().createObjectNode();
        parentNode.set("Class", classNode);
        Map<String, JsonNode> parentData = new HashMap<>();
        parentData.put("ParentNode", parentNode);
        doReturn(parentData).when(caseDataContent).getData();

        final Map<String, JsonNode> result =
            validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, caseDataContent);

        assertAll(
            () -> assertThat(result, is(parentData))
        );
    }

    @Test
    void should_fail_validate_when_organisation_has_null_role() throws Exception {
        List<CaseFieldValidationError> fieldErrors = new ArrayList<>();
        CaseValidationException caseValidationException = new CaseValidationException(fieldErrors);
        doThrow(caseValidationException)
            .when(caseTypeService)
            .validateData(any(ValidationContext.class));

        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(buildCaseType());

        final Map<String, JsonNode> organisationPolicyData =
            buildJsonNodeDataWithOrganisationPolicyRole(null);
        doReturn(organisationPolicyData).when(caseDataContent).getData();

        CaseValidationException caseValidationExceptionResult =
            assertThrows(CaseValidationException.class,
                () -> validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, caseDataContent));

        assertThat(caseValidationExceptionResult.getMessage(),
            containsString("Case data validation failed"));
    }

    @Test
    void should_fail_validate_when_organisation_has_incorrect_role() throws Exception {

        List<CaseFieldValidationError> fieldErrors = new ArrayList<>();
        CaseValidationException caseValidationException = new CaseValidationException(fieldErrors);
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(buildCaseType());

        doThrow(caseValidationException)
            .when(caseTypeService)
            .validateData(any(ValidationContext.class));

        final Map<String, JsonNode> organisationPolicyData =
            buildJsonNodeDataWithOrganisationPolicyRole("incorrect_role");
        doReturn(organisationPolicyData).when(caseDataContent).getData();

        CaseValidationException caseValidationExceptionResult =
            assertThrows(CaseValidationException.class,
                () -> validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, caseDataContent));


        assertThat(caseValidationExceptionResult.getMessage(),
            containsString("Case data validation failed"));
    }

    @Test
    void shouldValidateData_when_organisation_has_correct_role() throws Exception {
        CaseTypeDefinition caseTypeDefinition = buildCaseType();
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(caseTypeDefinition);

        final Map<String, JsonNode> organisationPolicyData =
            buildJsonNodeDataWithOrganisationPolicyRole("[default_role1]");
        doReturn(organisationPolicyData).when(caseDataContent).getData();

        validateCaseFieldsOperation.validateData(organisationPolicyData, caseTypeDefinition, caseDataContent);
    }

    @Test
    void should_fail_validateData_when_organisation_has_null_role() throws Exception {
        List<CaseFieldValidationError> fieldErrors = new ArrayList<>();
        CaseValidationException caseValidationException = new CaseValidationException(fieldErrors);
        doThrow(caseValidationException)
            .when(caseTypeService)
            .validateData(any(ValidationContext.class));

        CaseTypeDefinition caseTypeDefinition = buildCaseType();
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(caseTypeDefinition);

        final Map<String, JsonNode> organisationPolicyData = buildJsonNodeDataWithOrganisationPolicyRole(null);
        doReturn(organisationPolicyData).when(caseDataContent).getData();

        CaseValidationException caseValidationExceptionResult =
            assertThrows(CaseValidationException.class,
                () -> validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, caseDataContent));


        assertThat(caseValidationExceptionResult.getMessage(),
            containsString("Case data validation failed"));
    }

    @Test
    void should_fail_validateData_when_organisation_has_incorrect_role() throws Exception {
        CaseTypeDefinition caseTypeDefinition = buildCaseType();
        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(caseTypeDefinition);
        List<CaseFieldValidationError> fieldErrors = new ArrayList<>();
        CaseValidationException caseValidationException = new CaseValidationException(fieldErrors);
        doThrow(caseValidationException)
            .when(caseTypeService)
            .validateData(any(ValidationContext.class));

        final Map<String, JsonNode> organisationPolicyData =
            buildJsonNodeDataWithOrganisationPolicyRole("incorrect_role");
        doReturn(organisationPolicyData).when(caseDataContent).getData();

        CaseValidationException caseValidationExceptionResult =
            assertThrows(CaseValidationException.class,
                () -> validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, caseDataContent));

        assertThat(caseValidationExceptionResult.getMessage(),
            containsString("Case data validation failed"));
    }

    @Test
    void shouldValidateCaseDetails() {
        Map<String, JsonNode> result = validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, caseDataContent);

        assertAll(
            () -> verify(caseDefinitionRepository, times(1)).getCaseType(CASE_TYPE_ID),
            () -> verify(caseTypeService).validateData(any(ValidationContext.class)),
            () -> assertThat(result, is(data))
        );
    }

    @Test
    void shouldFailValidationIfNoContent() {
        ValidationException exception =
            assertThrows(ValidationException.class, () -> validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID,
                null));
        assertThat(exception.getMessage(),
            startsWith("Cannot validate case field because of event is not specified"));
        verify(caseDefinitionRepository, never()).getCaseType(any());
        verify(caseTypeService, never()).validateData(anyMap(), any());
    }

    @Test
    void shouldFailValidationIfNoEventInContent() {
        doReturn(null).when(caseDataContent).getEvent();

        ValidationException exception =
            assertThrows(ValidationException.class, () -> validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID,
                caseDataContent));
        assertThat(exception.getMessage(),
            startsWith("Cannot validate case field because of event is not specified"));
        verify(caseDefinitionRepository, never()).getCaseType(any());
        verify(caseTypeService, never()).validateData(anyMap(), any());
    }

    @Test
    void shouldFailValidationIfNoEventIdInContent() {
        doReturn(null).when(caseDataContent).getEventId();

        ValidationException exception =
            assertThrows(ValidationException.class, () -> validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID,
                caseDataContent));
        assertThat(exception.getMessage(),
            startsWith("Cannot validate case field because of event is not specified"));
        verify(caseDefinitionRepository, never()).getCaseType(any());
        verify(caseTypeService, never()).validateData(anyMap(), any());
    }

    @Test
    void shouldFailValidationIfNoCaseType() {
        doReturn(null).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);

        ValidationException exception =
            assertThrows(ValidationException.class, () -> validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID,
                caseDataContent));
        assertThat(exception.getMessage(),
            startsWith("Cannot find case type definition for " + CASE_TYPE_ID));
        verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        verify(caseTypeService, never()).validateData(anyMap(), any());
    }

    @Test
    void shouldFailValidationIfEventIdNotFoundInCaseType() {
        doReturn("otherEvent").when(caseDataContent).getEventId();

        ValidationException exception =
            assertThrows(ValidationException.class, () -> validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID,
                caseDataContent));
        assertThat(exception.getMessage(),
            startsWith("Cannot validate case field because of event otherEvent is not found in case type definition"));
        verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        verify(caseTypeService, never()).validateData(anyMap(), any());
    }

    private Map<String, JsonNode> buildJsonNodeDataWithOrganisationPolicyRole(String organisationPolicyRole)
        throws IOException {
        final JsonNode node = new ObjectMapper().readTree("{\n"
            + "  \"OrgPolicyCaseAssignedRole\": \"" + organisationPolicyRole + "\",\n"
            + "  \"OrgPolicyReference\": \"" + organisationPolicyRole + "\",\n"
            + "  \"Organisation\": {\n"
            + "    \"OrganisationID\": \"OrganisationID\",\n"
            + "    \"OrganisationName\": " + "\"OrganisationName\"\n"
            + "  }\n"
            + "}\n");
        final Map<String, JsonNode> map = new HashMap<>();
        map.put("OrganisationPolicyField", node);
        return map;
    }

    private static CaseTypeDefinition buildCaseType() {
        final List<CaseEventDefinition> CaseEventDefinitions = new ArrayList<>();
        final List<CaseEventFieldDefinition> caseEventFieldDefinitions = new ArrayList<>();
        final List<CaseEventFieldComplexDefinition> caseEventFieldComplexDefinitions = new ArrayList<>();
        final JurisdictionDefinition j = buildJurisdiction();
        final Version version = new Version();
        version.setNumber(67);
        final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        final CaseEventFieldDefinition caseEventFieldDefinition = new CaseEventFieldDefinition();
        caseEventFieldDefinition.setCaseFieldId("OrganisationPolicyField");
        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        final CaseEventFieldComplexDefinition caseEventFieldComplexDefinition = new CaseEventFieldComplexDefinition();
        caseEventFieldComplexDefinition.setRetainHiddenValue(Boolean.FALSE);
        caseEventFieldComplexDefinition.setReference(ORGANISATION_POLICY_ROLE);

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

    private CaseTypeDefinition buildCaseTypeWithTwoDefaultValues(String defaultValue1, String defaultValue2) {
        return buildCaseTypeWithTwoDefaultValues(defaultValue1,
            defaultValue2,
            ORGANISATION_POLICY_ROLE,
            ORGANISATION_POLICY_ROLE,
            ORGANISATIONPOLICYFIELD_1,
            ORGANISATIONPOLICYFIELD_2);
    }


    private CaseTypeDefinition buildCaseTypeWithTwoDefaultValues(String defaultValue1,
                                                                 String defaultValue2,
                                                                 String reference1,
                                                                 String reference2,
                                                                 String caseFieldId1,
                                                                 String caseFieldId2) {
        final List<CaseEventDefinition> caseEventDefinitions = new ArrayList();
        final List<CaseEventFieldDefinition> caseEventFieldDefinitions = new ArrayList<>();
        final List<CaseEventFieldComplexDefinition> caseEventFieldComplexDefinitions1 = new ArrayList<>();
        final JurisdictionDefinition j = buildJurisdiction();
        final Version version = new Version();
        version.setNumber(67);
        final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();

        //---1 ---/
        final CaseEventFieldDefinition caseEventFieldDefinition1 = new CaseEventFieldDefinition();
        caseEventFieldDefinition1.setCaseFieldId(caseFieldId1);

        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();

        final CaseEventFieldComplexDefinition caseEventFieldComplexDefinition1 = new CaseEventFieldComplexDefinition();
        caseEventFieldComplexDefinition1.setDefaultValue(defaultValue1);
        caseEventFieldComplexDefinition1.setRetainHiddenValue(Boolean.FALSE);
        caseEventFieldComplexDefinition1.setReference(reference1);

        caseEventFieldComplexDefinitions1.add(caseEventFieldComplexDefinition1);
        caseEventFieldDefinition1.setCaseEventFieldComplexDefinitions(caseEventFieldComplexDefinitions1);
        caseEventFieldDefinitions.add(caseEventFieldDefinition1);

        //----2 ----/
        final List<CaseEventFieldComplexDefinition> caseEventFieldComplexDefinitions2 = new ArrayList<>();

        final CaseEventFieldDefinition caseEventFieldDefinition2 = new CaseEventFieldDefinition();
        caseEventFieldDefinition2.setCaseFieldId(caseFieldId2);

        final CaseEventFieldComplexDefinition caseEventFieldComplexDefinition2 = new CaseEventFieldComplexDefinition();
        caseEventFieldComplexDefinition2.setDefaultValue(defaultValue2);
        caseEventFieldComplexDefinition2.setRetainHiddenValue(Boolean.FALSE);
        caseEventFieldComplexDefinition2.setReference(reference2);
        caseEventFieldComplexDefinitions2.add(caseEventFieldComplexDefinition2);
        caseEventFieldDefinition2.setCaseEventFieldComplexDefinitions(caseEventFieldComplexDefinitions2);
        caseEventFieldDefinitions.add(caseEventFieldDefinition2);

        //---General ---/
        caseEventDefinition.setId("eventId");
        caseEventDefinition.setCaseFields(caseEventFieldDefinitions);
        caseTypeDefinition.setId("caseTypeId");
        caseTypeDefinition.setName("case type name");
        caseTypeDefinition.setJurisdictionDefinition(j);
        caseTypeDefinition.setVersion(version);
        caseEventDefinitions.add(caseEventDefinition);
        caseTypeDefinition.setEvents(caseEventDefinitions);
        return caseTypeDefinition;
    }

    private static JurisdictionDefinition buildJurisdiction() {
        final JurisdictionDefinition j = new JurisdictionDefinition();
        j.setId(JURISDICTION_ID);
        return j;
    }

    private Map<String, JsonNode> buildJsonNodeDataWithTwoOrganisationPolicyRole(String organisationPolicyRole1,
                                                                                 String organisationPolicyRole2)
        throws IOException {
        final JsonNode node = new ObjectMapper().readTree("{\n"
            + "  \"OrgPolicyCaseAssignedRole\": \"" + organisationPolicyRole1 + "\",\n"
            + "  \"OrgPolicyReference\": \"" + organisationPolicyRole1 + "\",\n"
            + "  \"Organisation\": {\n"
            + "    \"OrganisationID\": \"OrganisationID\",\n"
            + "    \"OrganisationName\": " + "\"OrganisationName\"\n"
            + "  }\n"
            + "}\n");

        final JsonNode node1 = new ObjectMapper().readTree("{\n"
            + "  \"OrgPolicyCaseAssignedRole\": \"" + organisationPolicyRole2 + "\",\n"
            + "  \"OrgPolicyReference\": \"" + organisationPolicyRole2 + "\",\n"
            + "  \"Organisation\": {\n"
            + "    \"OrganisationID\": \"OrganisationID\",\n"
            + "    \"OrganisationName\": " + "\"OrganisationName\"\n"
            + "  }\n"
            + "}\n");

        final Map<String, JsonNode> map = new HashMap<>();
        map.put(ORGANISATIONPOLICYFIELD_1, node);
        map.put(ORGANISATIONPOLICYFIELD_2, node1);
        return map;
    }
}
