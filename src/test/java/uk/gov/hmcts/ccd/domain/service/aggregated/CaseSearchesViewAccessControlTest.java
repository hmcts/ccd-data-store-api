package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResultDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationServiceImpl;
import uk.gov.hmcts.ccd.domain.service.processor.date.DateTimeSearchResultProcessor;
import uk.gov.hmcts.ccd.domain.service.search.CaseSearchesViewAccessControl;
import uk.gov.hmcts.ccd.domain.service.search.SearchResultDefinitionService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.SearchResultBuilder.searchResult;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.buildData;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.buildSearchResultField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.AccessControlListBuilder.anAcl;

class CaseSearchesViewAccessControlTest {

    private static final String CASE_TYPE_ID_1 = "CASE_TYPE_1";
    private static final String JURISDICTION = "JURISDICTION";
    private static final String CASE_FIELD_1 = "Case field 1";
    private static final String CASE_FIELD_2 = "Case field 2";
    private static final String CASE_FIELD_3 = "Case field 3";
    private static final String CASE_FIELD_4 = "Case field 4";
    private static final String CASE_FIELD_5 = "Case field 5";
    private static final String ROLE_IN_USER_ROLE_1 = "Role 1";
    private static final String ROLE_IN_USER_ROLE_2 = "Role 2";

    private static final String FAMILY_DETAILS = "FamilyDetails";
    private static final String FATHER_NAME_VALUE = "Simmon";
    private static final String MOTHER_NAME_VALUE = "Hanna";
    private static final String POSTCODE_VALUE = "SW1P 4ER";
    private static final String FAMILY_DETAILS_VALUE = "{\"FatherName\":\"" + FATHER_NAME_VALUE + "\","
        + "\"MotherName\":\"" + MOTHER_NAME_VALUE + "\","
        + "\"FamilyAddress\":{"
        + "\"County\":\"\","
        + "\"Country\":\"United Kingdom\","
        + "\"PostCode\":\"" + POSTCODE_VALUE + "\","
        + "\"PostTown\":\"London\","
        + "\"AddressLine1\":\"40 Edric House\","
        + "\"AddressLine2\":\"\",\"AddressLine3\":\"\"}"
        + "}";
    private static final String POSTCODE = "PostCode";

    private static final String TEXT_TYPE = "Text";

    private static final SecurityClassification SECURITY_CLASSIFICATION = SecurityClassification.PUBLIC;

    @Mock
    private CaseDataAccessControl caseDataAccessControl;

    @Mock
    private CaseTypeService caseTypeService;

    @Mock
    private SearchResultDefinitionService searchResultDefinitionService;

    @Mock
    private SecurityClassificationServiceImpl securityClassificationService;

    @Mock
    private DateTimeSearchResultProcessor dateTimeSearchResultProcessor;

    private CaseSearchesViewAccessControl classUnderTest;

    private Map<String, JsonNode> dataMap;
    private JurisdictionDefinition jurisdiction;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);

        dataMap = buildData(CASE_FIELD_1, CASE_FIELD_2, CASE_FIELD_3, CASE_FIELD_4, CASE_FIELD_5);
        ObjectNode familyDetails = (ObjectNode) new ObjectMapper().readTree(FAMILY_DETAILS_VALUE);
        dataMap.put(FAMILY_DETAILS, familyDetails);

        jurisdiction = JurisdictionDefinition.builder()
            .id(JURISDICTION)
            .build();
        CaseTypeDefinition caseTypeDefinition1 = caseTypeDefinitionWithThreeFieldsAndReadACLs();

        when(caseTypeService.getCaseType(eq(CASE_TYPE_ID_1))).thenReturn(caseTypeDefinition1);

        SearchResultDefinition caseType1SearchResult = searchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_1, "", CASE_FIELD_1, "", ROLE_IN_USER_ROLE_1),
                buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_2, "", CASE_FIELD_2, "", ""))
            .build();

        when(searchResultDefinitionService.getSearchResultDefinition(any(), any(), any()))
            .thenReturn(caseType1SearchResult);
        doAnswer(i -> i.getArgument(1)).when(dateTimeSearchResultProcessor).execute(any(), any());
        when(securityClassificationService.userHasEnoughSecurityClassificationForField(any(), any(), any()))
            .thenReturn(true);

        classUnderTest = new CaseSearchesViewAccessControl(caseTypeService,
            searchResultDefinitionService, securityClassificationService, caseDataAccessControl);

    }

    @Test
    void shouldReturnTrueForFilterResultsBySearchResultsDefinition() {
        CaseTypeDefinition caseTypeDefinition1 = CaseTypeDefinition.builder()
            .id(CASE_TYPE_ID_1)
            .jurisdictionDefinition(jurisdiction)
            .securityClassification(SecurityClassification.PUBLIC)
            .caseFieldDefinitions(List.of(
                CaseFieldDefinition.builder().id(CASE_FIELD_1).fieldTypeDefinition(textFieldType())
                    .caseTypeId(CASE_TYPE_ID_1)
                    .accessControlLists(List.of(anAcl()
                        .withRole(ROLE_IN_USER_ROLE_1)
                        .withRead(true)
                        .build())).build(),
                CaseFieldDefinition.builder().id(CASE_FIELD_2).fieldTypeDefinition(textFieldType())
                    .caseTypeId(CASE_TYPE_ID_1)
                    .accessControlLists(List.of(anAcl()
                        .withRole(ROLE_IN_USER_ROLE_1)
                        .withRead(true)
                        .build())).build(),
                CaseFieldDefinition.builder().id(CASE_FIELD_3).fieldTypeDefinition(textFieldType())
                    .caseTypeId(CASE_TYPE_ID_1)
                    .accessControlLists(List.of(anAcl()
                        .withRole(ROLE_IN_USER_ROLE_1)
                        .withRead(true)
                        .build())).build()
            ))
            .build();

        mockAccessProfiles();
        List<String> requestedFields = new ArrayList<>();

        assertTrue(classUnderTest
            .filterResultsBySearchResultsDefinition("ORGCASES", caseTypeDefinition1, requestedFields, CASE_FIELD_1));
    }

    @Test
    void shouldReturnTrueForFilterResultsBySearchResultsDefinitionWhenUseCaseIsNull() {
        CaseTypeDefinition caseTypeDefinition1 = caseTypeDefinitionWithThreeFieldsAndReadACLs();

        mockAccessProfiles();
        List<String> requestedFields = new ArrayList<>();

        assertTrue(classUnderTest
            .filterResultsBySearchResultsDefinition(null, caseTypeDefinition1, requestedFields, CASE_FIELD_1));
    }

    @Test
    void shouldReturnFalseForFilterResultsBySearchResultsDefinition() {
        CaseTypeDefinition caseTypeDefinition1 = caseTypeDefinitionWithThreeFieldsAndReadACLs();

        mockAccessProfiles();
        List<String> requestedFields = new ArrayList<>();


        assertFalse(classUnderTest
            .filterResultsBySearchResultsDefinition("ORGCASES", caseTypeDefinition1, requestedFields, CASE_FIELD_2));
    }

    @Test
    void shouldReturnTrueForFilterFieldByAuthorisationAccessOnField() {
        final CaseFieldDefinition postCode = CaseFieldDefinition.builder().id(POSTCODE)
            .fieldTypeDefinition(textFieldType())
            .caseTypeId(CASE_TYPE_ID_1)
            .securityLabel(SECURITY_CLASSIFICATION.name())
            .accessControlLists(List.of(anAcl()
                .withRole(ROLE_IN_USER_ROLE_1)
                .withRead(true)
                .build())).build();

        mockAccessProfiles();
        assertTrue(classUnderTest.filterFieldByAuthorisationAccessOnField(postCode));
    }

    @Test
    void shouldReturnFalseForFilterFieldByAuthorisationAccessOnField() {
        final CaseFieldDefinition postCode = CaseFieldDefinition.builder().id(POSTCODE)
            .fieldTypeDefinition(textFieldType())
            .securityLabel(SECURITY_CLASSIFICATION.name())
            .accessControlLists(List.of(anAcl()
                .withRole(ROLE_IN_USER_ROLE_1)
                .withRead(true)
                .build())).build();

        mockAccessProfiles(ROLE_IN_USER_ROLE_2);

        assertFalse(classUnderTest.filterFieldByAuthorisationAccessOnField(postCode));
    }

    @Test
    void shouldReturnTrueForFilterResultsBySecurityClassification() {
        final CaseFieldDefinition caseFieldDefinition1 = CaseFieldDefinition.builder().id(CASE_FIELD_1)
            .fieldTypeDefinition(textFieldType())
            .securityLabel(SecurityClassification.PRIVATE.name())
            .accessControlLists(List.of(anAcl()
                .withRole(ROLE_IN_USER_ROLE_1)
                .withRead(true)
                .build())).build();

        CaseTypeDefinition caseTypeDefinition1 = caseTypeDefinitionWithThreeFieldsAndReadACLs();

        mockAccessProfiles();

        assertTrue(classUnderTest.filterResultsBySecurityClassification(caseFieldDefinition1, caseTypeDefinition1));
    }

    @Test
    void shouldReturnFalseForFilterResultsBySecurityClassification() {
        final CaseFieldDefinition caseFieldDefinition1 = CaseFieldDefinition.builder().id(CASE_FIELD_1)
            .fieldTypeDefinition(textFieldType())
            .securityLabel(SecurityClassification.PUBLIC.name())
            .accessControlLists(List.of(anAcl()
                .withRole(ROLE_IN_USER_ROLE_1)
                .withRead(true)
                .build())).build();

        CaseTypeDefinition caseTypeDefinition1 = caseTypeDefinitionWithThreeFieldsAndReadACLs();


        mockAccessProfiles();
        when(securityClassificationService.userHasEnoughSecurityClassificationForField(any(), any(), any()))
            .thenReturn(false);
        assertFalse(classUnderTest.filterResultsBySecurityClassification(caseFieldDefinition1, caseTypeDefinition1));
    }

    private CaseTypeDefinition caseTypeDefinitionWithThreeFieldsAndReadACLs() {
        return CaseTypeDefinition.builder()
            .id(CASE_TYPE_ID_1)
            .jurisdictionDefinition(jurisdiction)
            .securityClassification(SecurityClassification.PUBLIC)
            .caseFieldDefinitions(List.of(
                CaseFieldDefinition.builder().id(CASE_FIELD_1).fieldTypeDefinition(textFieldType())
                    .accessControlLists(List.of(anAcl()
                        .withRole(ROLE_IN_USER_ROLE_1)
                        .withRead(true)
                        .build())).build(),
                CaseFieldDefinition.builder().id(CASE_FIELD_2).fieldTypeDefinition(textFieldType())
                    .accessControlLists(List.of(anAcl()
                        .withRole(ROLE_IN_USER_ROLE_1)
                        .withRead(true)
                        .build())).build(),
                CaseFieldDefinition.builder().id(CASE_FIELD_3).fieldTypeDefinition(textFieldType())
                    .accessControlLists(List.of(anAcl()
                        .withRole(ROLE_IN_USER_ROLE_1)
                        .withRead(true)
                        .build())).build()
            ))
            .build();
    }

    private void mockAccessProfiles() {
        mockAccessProfiles(ROLE_IN_USER_ROLE_1, ROLE_IN_USER_ROLE_2);
    }

    private void mockAccessProfiles(String... roles) {
        when(caseDataAccessControl.generateAccessProfilesByCaseTypeId(anyString()))
            .thenReturn(createAccessProfiles(Sets.newHashSet(roles)));
    }

    private Set<AccessProfile> createAccessProfiles(Set<String> userRoles) {
        return userRoles.stream()
            .map(userRole -> AccessProfile.builder().readOnly(false)
                .accessProfile(userRole)
                .build())
            .collect(Collectors.toSet());
    }


    private FieldTypeDefinition textFieldType() {
        return FieldTypeDefinition.builder().id(TEXT_TYPE).type(TEXT_TYPE).build();
    }
}
