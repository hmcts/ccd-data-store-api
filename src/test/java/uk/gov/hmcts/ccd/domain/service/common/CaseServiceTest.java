package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldComplexDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

class CaseServiceTest {

    private static final String JURISDICTION = "SSCS";
    private static final String STATE = "CreatedState";
    private static final String CASE_REFERENCE = "1234123412341236";
    private static final Long REFERENCE = Long.valueOf(CASE_REFERENCE);
    private static final String DATA_PERSON = "Person";
    private static final String DATA_NAMES = "Names";
    private static final String DATA_FNAME = "FirstName";
    private static final String PERSON_FNAME = "Jack";
    private static final String OTHER_NAME = "John";
    private static final String CASE_ID = "299";
    private final Map<String, JsonNode> data = new HashMap<>();
    private static final ObjectMapper MAPPER = JacksonUtils.MAPPER;
    private CaseDataService caseDataService;

    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    @Mock
    private UIDService uidService;

    private CaseService caseService;
    private CaseDetails caseDetails;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(true).when(uidService).validateUID(CASE_REFERENCE);
        caseDetails = buildCaseDetails();
        caseDetails.setId(CASE_ID);
        doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findByReference(JURISDICTION, REFERENCE);
        doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findByReferenceWithNoAccessControl(
            CASE_REFERENCE);

        caseDataService = new CaseDataService();
        caseService = new CaseService(caseDataService, caseDetailsRepository, uidService);
    }

    @Nested
    @DisplayName("getCaseDetails()")
    class GetCaseDetails {
        @Test
        @DisplayName("should return caseDetails")
        void getCaseDetails() {

            CaseDetails result = caseService.getCaseDetails(JURISDICTION, CASE_REFERENCE);
            assertAll(
                () -> assertThat(result.getId(), is(caseDetails.getId())),
                () -> verify(caseDetailsRepository).findByReference(JURISDICTION, REFERENCE),
                () -> verify(uidService).validateUID(CASE_REFERENCE)
            );
        }

        @Test
        @DisplayName("should fail for bad CASE_REFERENCE")
        void shoudThrowBadRequestException() {
            doThrow(new BadRequestException("...")).when(uidService).validateUID(CASE_REFERENCE);

            assertThrows(BadRequestException.class, () -> caseService.getCaseDetails(JURISDICTION, CASE_REFERENCE));
        }

        @Test
        @DisplayName("should fail when case isn't found in the DB")
        void shoudThrowResourceNotFoundException() {
            doReturn(Optional.empty()).when(caseDetailsRepository).findByReference(JURISDICTION, REFERENCE);

            assertThrows(ResourceNotFoundException.class, () -> caseService.getCaseDetails(JURISDICTION,
                CASE_REFERENCE));
        }
    }

    @Nested
    @DisplayName("getCaseDetailsByCaseReference()")
    class GetCaseDetailsByCaseReference {
        @Test
        @DisplayName("should return caseDetails")
        void getCaseDetails() {

            CaseDetails result = caseService.getCaseDetailsByCaseReference(CASE_REFERENCE);
            assertAll(
                () -> assertThat(result.getId(), is(caseDetails.getId())),
                () -> verify(caseDetailsRepository).findByReferenceWithNoAccessControl(CASE_REFERENCE)
            );
        }

        @Test
        @DisplayName("should fail when case isn't found in the DB")
        void shoudThrowResourceNotFoundException() {
            doReturn(Optional.empty()).when(caseDetailsRepository).findByReferenceWithNoAccessControl(CASE_REFERENCE);

            assertThrows(ResourceNotFoundException.class, () -> caseService.getCaseDetailsByCaseReference(
                CASE_REFERENCE));
        }
    }


    @Nested
    @DisplayName("clone()")
    class Clone {

        @Test
        @DisplayName("should clone case details")
        void shouldCloneCaseDetails() {
            final CaseDetails caseDetails = buildCaseDetails();

            final CaseDetails clone = caseService.clone(caseDetails);

            assertAll(
                () -> assertThat(clone, not(sameInstance(caseDetails))),
                () -> assertThat(clone.getJurisdiction(), equalTo(JURISDICTION)),
                () -> assertThat(clone.getReference(), equalTo(REFERENCE)),
                () -> assertThat(clone.getState(), equalTo(STATE))
            );
        }

        @Test
        @DisplayName("should deep clone case data")
        void shouldDeepCloneCaseData() {
            final CaseDetails caseDetails = buildCaseDetails();

            final CaseDetails clone = caseService.clone(caseDetails);

            final Map<String, JsonNode> data = caseDetails.getData();
            final Map<String, JsonNode> cloneData = clone.getData();

            final JsonNode person = data.get(DATA_PERSON);
            final JsonNode clonePerson = cloneData.get(DATA_PERSON);

            final JsonNode names = person.get(DATA_NAMES);
            final JsonNode cloneNames = clonePerson.get(DATA_NAMES);

            // Change name
            ((ObjectNode) names).set(DATA_FNAME, new TextNode(OTHER_NAME));

            final JsonNode fname = names.get(DATA_FNAME);
            final JsonNode cloneFname = cloneNames.get(DATA_FNAME);

            assertAll(
                () -> assertThat(cloneData, not(sameInstance(data))),
                () -> assertThat(clonePerson, not(sameInstance(person))),
                () -> assertThat(cloneNames, not(sameInstance(names))),
                () -> assertThat(cloneFname, not(sameInstance(fname))),
                () -> assertThat(cloneFname.asText(), equalTo(PERSON_FNAME)),
                () -> assertThat(fname.asText(), equalTo(OTHER_NAME))
            );
        }

        @Test
        @DisplayName("should deep clone case data classification")
        void shouldDeepCloneCaseDataClassification() {
            final CaseDetails caseDetails = buildCaseDetails();

            final CaseDetails clone = caseService.clone(caseDetails);

            final Map<String, JsonNode> classification = caseDetails.getDataClassification();
            final Map<String, JsonNode> cloneClassification = clone.getDataClassification();

            final JsonNode person = classification.get(DATA_PERSON);
            final JsonNode clonePerson = cloneClassification.get(DATA_PERSON);

            final JsonNode names = person.get(DATA_NAMES);
            final JsonNode cloneNames = clonePerson.get(DATA_NAMES);

            // Change name classification
            ((ObjectNode) names).set(DATA_FNAME, new TextNode("PRIVATE"));

            final JsonNode fname = names.get(DATA_FNAME);
            final JsonNode cloneFname = cloneNames.get(DATA_FNAME);

            assertAll(
                () -> assertThat(cloneClassification, not(sameInstance(classification))),
                () -> assertThat(clonePerson, not(sameInstance(person))),
                () -> assertThat(cloneNames, not(sameInstance(names))),
                () -> assertThat(cloneFname, not(sameInstance(fname))),
                () -> assertThat(cloneFname.asText(), equalTo("PUBLIC")),
                () -> assertThat(fname.asText(), equalTo("PRIVATE"))
            );
        }
    }

    @Nested
    @DisplayName("populateCurrentCaseDetailsWithUserInputs()")
    class PopulateCurrentCaseDetailsWithUserInputs {
        @Test
        @DisplayName("should return caseDetails")
        void populateCurrentCaseDetailsWithEventFields() throws Exception {


            Map<String, JsonNode> eventData = JacksonUtils.convertValue(MAPPER.readTree(
                "{\n"
                    + "  \"PersonFirstName\": \"First Name\",\n"
                    + "  \"PersonLastName\": \"Last Name\"\n"
                    + "}"));

            Map<String, JsonNode> resultData = JacksonUtils.convertValue(MAPPER.readTree(
                "{\n"
                    + "  \"PersonFirstName\": \"First Name\",\n"
                    + "  \"PersonLastName\": \"Last Name\",\n"
                    + "  \"Person\":{\"Names\":{\"FirstName\":\"Jack\"}}\n"
                    + "}"));

            CaseDataContent caseDataContent = newCaseDataContent()
                .withCaseReference(CASE_REFERENCE)
                .withEventData(eventData)
                .build();
            CaseDetails caseDetails = buildCaseDetails();
            caseDetails.setId("299");
            CaseDetails result = caseService.populateCurrentCaseDetailsWithEventFields(caseDataContent, caseDetails);

            assertAll(
                () -> assertThat(result.getId(), is(CaseServiceTest.this.caseDetails.getId())),
                () -> assertThat(result.getData(), is(resultData))
            );
        }

        @Test
        @DisplayName("should fail for bad CASE_REFERENCE")
        void shoudThrowBadRequestException() {
            doThrow(new BadRequestException("...")).when(uidService).validateUID(CASE_REFERENCE);

            assertThrows(BadRequestException.class, () -> caseService.getCaseDetails(JURISDICTION, CASE_REFERENCE));
        }

        @Test
        @DisplayName("should fail when case isn't found in the DB")
        void shoudThrowResourceNotFoundException() {
            doReturn(Optional.empty()).when(caseDetailsRepository).findByReference(JURISDICTION, REFERENCE);

            assertThrows(ResourceNotFoundException.class, () -> caseService.getCaseDetails(JURISDICTION,
                CASE_REFERENCE));
        }
    }

    @Nested
    @DisplayName("buildJsonFromCaseFieldsWithDefaultValue()")
    class BuildJsonFromCaseFieldsWithDefaultValue {
        @Test
        @DisplayName("builds a Json representation from CaseEventDefinition caseFields")
        void buildsJsonRepresentationFromEventCaseFields() throws Exception {

            final List<CaseEventFieldDefinition> caseFields = Arrays.asList(
                TestBuildersUtil.CaseEventFieldDefinitionBuilder.newCaseEventField()
                    .withCaseFieldId("ChangeOrganisationRequestField")
                    .addCaseEventFieldComplexDefinitions(CaseEventFieldComplexDefinition.builder()
                                                             .reference("Reason")
                                                             .defaultValue("SomeReasonX")
                                                             .build())
                    .addCaseEventFieldComplexDefinitions(CaseEventFieldComplexDefinition.builder()
                                                             .reference("CaseRoleId")
                                                             .defaultValue(null)
                                                             .build())
                    .addCaseEventFieldComplexDefinitions(CaseEventFieldComplexDefinition.builder()
                                                             .reference("OrganisationToAdd.OrganisationID")
                                                             .defaultValue("Solicitor firm 1")
                                                             .build())
                    .build(),
                TestBuildersUtil.CaseEventFieldDefinitionBuilder.newCaseEventField()
                    .withCaseFieldId("OrganisationPolicyField")
                    .addCaseEventFieldComplexDefinitions(CaseEventFieldComplexDefinition.builder()
                                                             .reference("OrgPolicyCaseAssignedRole")
                                                             .defaultValue("[Claimant]")
                                                             .build())
                    .build()
            );

            Map<String, JsonNode> result = caseService.buildJsonFromCaseFieldsWithDefaultValue(caseFields);

            assertAll(
                () -> assertThat(result.size(), is(2)),

                () -> assertTrue(result.containsKey("ChangeOrganisationRequestField")),
                () -> assertNotNull(result.get("ChangeOrganisationRequestField").get("Reason")),
                () -> assertNull(result.get("ChangeOrganisationRequestField").get("CaseRoleId")),
                () -> assertNotNull(result.get("ChangeOrganisationRequestField").get("OrganisationToAdd")
                                        .get("OrganisationID")),
                () -> assertThat(result.get("ChangeOrganisationRequestField").get("Reason").asText(),
                                 is("SomeReasonX")),
                () -> assertThat(result.get("ChangeOrganisationRequestField").get("OrganisationToAdd")
                                     .get("OrganisationID").asText(), is("Solicitor firm 1")),

                () -> assertTrue(result.containsKey("OrganisationPolicyField")),
                () -> assertNotNull(result.get("OrganisationPolicyField").get("OrgPolicyCaseAssignedRole")),
                () -> assertThat(result.get("OrganisationPolicyField").get("OrgPolicyCaseAssignedRole").asText(),
                                 is("[Claimant]"))
            );
        }
    }

    private CaseDetails buildCaseDetails() {
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setJurisdiction(JURISDICTION);
        caseDetails.setReference(REFERENCE);
        caseDetails.setState(STATE);

        caseDetails.setData(buildCaseData());
        caseDetails.setDataClassification(buildCaseDataClassification());

        return caseDetails;
    }

    private Map<String, JsonNode> buildCaseDataStructure(String fnameValue) {
        final HashMap<String, JsonNode> data = new HashMap<>();
        final ObjectMapper objectMapper = new ObjectMapper();

        final ObjectNode namesNode = objectMapper.createObjectNode();
        namesNode.set(DATA_FNAME, new TextNode(fnameValue));

        final ObjectNode personNode = objectMapper.createObjectNode();
        personNode.set(DATA_NAMES, namesNode);

        data.put(DATA_PERSON, personNode);

        return data;
    }

    private Map<String, JsonNode> buildCaseData() {
        return buildCaseDataStructure(PERSON_FNAME);
    }

    private Map<String, JsonNode> buildCaseDataClassification() {
        return buildCaseDataStructure("PUBLIC");
    }

}
