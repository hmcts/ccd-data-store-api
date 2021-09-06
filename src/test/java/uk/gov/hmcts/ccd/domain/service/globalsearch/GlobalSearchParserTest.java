package uk.gov.hmcts.ccd.domain.service.globalsearch;

import org.hamcrest.core.Is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.search.global.SearchCriteria;
import uk.gov.hmcts.ccd.domain.model.search.global.SearchCriteriaResponse;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;

import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.TEXT;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.AccessControlListBuilder.anAcl;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class GlobalSearchParserTest {


    private static final String ROLE_IN_USER_ROLES = "caseworker-probate-loa1";
    private static final String ROLE_IN_USER_ROLES_2 = "caseworker-divorce-loa";
    private static final String ROLE_IN_USER_ROLE_1 = "Role 1";
    private static final Set<String> USER_ROLES = newHashSet(ROLE_IN_USER_ROLES,
        ROLE_IN_USER_ROLES_2, ROLE_IN_USER_ROLE_1);

    private static final String CASE_TYPE_ID_1 = "CASE_TYPE_1";
    private static final String CASE_TYPE_ID_2 = "CASE_TYPE_2";
    private static final String CASE_TYPE_ID_3 = "CASE_TYPE_3";
    private static final String JURISDICTION = "JURISDICTION";
    private static final String CASE_FIELD_1 = "Case field 1";
    private static final String CASE_FIELD_2 = "Case field 2";
    private static final String CASE_FIELD_3 = "Case field 3";
    private static final String TEXT_TYPE = "Text";

    @Mock
    private UserRepository userRepository;
    @Mock
    private CaseTypeService caseTypeService;

    private JurisdictionDefinition jurisdiction;
    private List<String> validFields;

    private GlobalSearchParser globalSearchParser;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        validFields = List.of("ValidEntry", "ValidEntryTwo");
        doReturn(USER_ROLES).when(userRepository).getUserRoles();

        jurisdiction = new JurisdictionDefinition();
        jurisdiction.setId(JURISDICTION);


        CaseTypeDefinition caseTypeDefinition1 = newCaseType()
            .withCaseTypeId(CASE_TYPE_ID_1)
            .withJurisdiction(jurisdiction)
            .withAcl(anAcl()
                .withRole(ROLE_IN_USER_ROLE_1)
                .withRead(true)
                .build())
            .withField(newCaseField().withId("caseManagementLocation")
                .withSC(SecurityClassification.PUBLIC.name())
                .withFieldType(
                    aFieldType()
                        .withId("caseManagementLocation")
                        .withType(COMPLEX)
                        .withComplexField(complexField("region", TEXT,
                            SecurityClassification.PUBLIC, ROLE_IN_USER_ROLE_1, true))
                        .withComplexField(complexField("baseLocation", TEXT,
                            SecurityClassification.PUBLIC, ROLE_IN_USER_ROLE_1, true))
                        .build())
                .build())
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .withField(newCaseField().withId(CASE_FIELD_2).withFieldType(textFieldType())
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLE_1)
                    .withRead(true)
                    .build()).build())
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .withField(newCaseField().withId(CASE_FIELD_3).withFieldType(textFieldType())
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLE_1)
                    .withRead(true)
                    .build()).build())
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .build();

        CaseTypeDefinition caseTypeDefinition2 = newCaseType()
            .withCaseTypeId(CASE_TYPE_ID_2)
            .withJurisdiction(jurisdiction)
            .withAcl(anAcl()
                .withRole(ROLE_IN_USER_ROLE_1)
                .withRead(false)
                .build())
            .withField(newCaseField().withId("caseManagementLocation")
                .withSC(SecurityClassification.PUBLIC.name())
                .withFieldType(
                    aFieldType()
                        .withId("caseManagementLocation")
                        .withType(COMPLEX)
                        .withComplexField(complexField("region", TEXT,
                            SecurityClassification.PUBLIC, ROLE_IN_USER_ROLE_1, false))
                        .withComplexField(complexField("baseLocation", TEXT,
                            SecurityClassification.PUBLIC, ROLE_IN_USER_ROLE_1, true))
                        .build())
                .build())
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .withField(newCaseField().withId(CASE_FIELD_2).withFieldType(textFieldType())
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLE_1)
                    .withRead(true)
                    .build()).build())
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .withField(newCaseField().withId(CASE_FIELD_3).withFieldType(textFieldType())
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLE_1)
                    .withRead(true)
                    .build()).build())
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .build();

        CaseTypeDefinition caseTypeDefinition3 = newCaseType()
            .withCaseTypeId(CASE_TYPE_ID_3)
            .withJurisdiction(jurisdiction)
            .withAcl(anAcl()
                .withRole(ROLE_IN_USER_ROLE_1)
                .withRead(false)
                .build())
            .withField(newCaseField().withId("caseManagementLocation")
                .withSC(SecurityClassification.PUBLIC.name())
                .withFieldType(
                    aFieldType()
                        .withId("caseManagementLocation")
                        .withType(COMPLEX)
                        .withComplexField(complexField("region", TEXT,
                            SecurityClassification.RESTRICTED, ROLE_IN_USER_ROLE_1, true))
                        .withComplexField(complexField("baseLocation", TEXT,
                            SecurityClassification.PUBLIC, ROLE_IN_USER_ROLE_1, true))
                        .build())
                .build())
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .withField(newCaseField().withId(CASE_FIELD_2).withFieldType(textFieldType())
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLE_1)
                    .withRead(true)
                    .build()).build())
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .withField(newCaseField().withId(CASE_FIELD_3).withFieldType(textFieldType())
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLE_1)
                    .withRead(true)
                    .build()).build())
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .build();

        when(caseTypeService.getCaseType(eq(CASE_TYPE_ID_1))).thenReturn(caseTypeDefinition1);
        when(caseTypeService.getCaseType(eq(CASE_TYPE_ID_2))).thenReturn(caseTypeDefinition2);
        when(caseTypeService.getCaseType(eq(CASE_TYPE_ID_3))).thenReturn(caseTypeDefinition3);

        globalSearchParser = new GlobalSearchParser(userRepository, caseTypeService);
    }

    @Test
    void shouldNotFilterAnyResults() {
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setCaseManagementBaseLocationIds(validFields);
        searchCriteria.setCaseManagementRegionIds(validFields);

        SearchCriteriaResponse searchCriteriaResponse = new SearchCriteriaResponse();
        searchCriteriaResponse.setCaseManagementBaseLocationId("value");
        searchCriteriaResponse.setCcdCaseTypeId(CASE_TYPE_ID_1);
        searchCriteriaResponse.setCaseManagementBaseLocationId(CASE_TYPE_ID_1);
        searchCriteriaResponse.setCaseManagementRegionId(CASE_TYPE_ID_1);

        List<SearchCriteriaResponse> response =
            globalSearchParser
                .filterCases(new java.util.ArrayList<>(java.util.Arrays.asList(searchCriteriaResponse)),
                    searchCriteria);

        assertAll(
            () -> assertThat(response.size(), Is.is(1))
        );

    }

    @Test
    void shouldFilterResultsWhenFieldDoesNotHaveReadPermission() {
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setCaseManagementBaseLocationIds(validFields);
        searchCriteria.setCaseManagementRegionIds(validFields);

        SearchCriteriaResponse searchCriteriaResponse = new SearchCriteriaResponse();
        searchCriteriaResponse.setCaseManagementBaseLocationId("value");
        searchCriteriaResponse.setCcdCaseTypeId(CASE_TYPE_ID_1);
        searchCriteriaResponse.setCaseManagementBaseLocationId(CASE_TYPE_ID_1);
        searchCriteriaResponse.setCaseManagementRegionId(CASE_TYPE_ID_1);

        SearchCriteriaResponse searchCriteriaResponse2 = new SearchCriteriaResponse();
        searchCriteriaResponse2.setCaseManagementBaseLocationId("value");
        searchCriteriaResponse2.setCcdCaseTypeId(CASE_TYPE_ID_2);
        searchCriteriaResponse2.setCaseManagementBaseLocationId(CASE_TYPE_ID_1);
        searchCriteriaResponse2.setCaseManagementRegionId(CASE_TYPE_ID_1);

        List<SearchCriteriaResponse> response =
            globalSearchParser.filterCases(new java.util.ArrayList<>(java.util.Arrays.asList(searchCriteriaResponse,
                searchCriteriaResponse2)), searchCriteria);

        assertAll(
            () -> assertThat(response.size(), Is.is(1))
        );

    }

    @Test
    void shouldFilterResultsWhenFieldIsRestricted() {
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setCaseManagementBaseLocationIds(validFields);
        searchCriteria.setCaseManagementRegionIds(validFields);

        SearchCriteriaResponse searchCriteriaResponse = new SearchCriteriaResponse();
        searchCriteriaResponse.setCaseManagementBaseLocationId("value");
        searchCriteriaResponse.setCcdCaseTypeId(CASE_TYPE_ID_1);
        searchCriteriaResponse.setCaseManagementBaseLocationId(CASE_TYPE_ID_1);
        searchCriteriaResponse.setCaseManagementRegionId(CASE_TYPE_ID_1);

        SearchCriteriaResponse searchCriteriaResponse2 = new SearchCriteriaResponse();
        searchCriteriaResponse2.setCaseManagementBaseLocationId("value");
        searchCriteriaResponse2.setCcdCaseTypeId(CASE_TYPE_ID_3);
        searchCriteriaResponse2.setCaseManagementBaseLocationId(CASE_TYPE_ID_1);
        searchCriteriaResponse2.setCaseManagementRegionId(CASE_TYPE_ID_1);

        List<SearchCriteriaResponse> response =
            globalSearchParser.filterCases(new java.util.ArrayList<>(java.util.Arrays.asList(searchCriteriaResponse,
                searchCriteriaResponse2)), searchCriteria);

        assertAll(
            () -> assertThat(response.size(), Is.is(1))
        );

    }

    private FieldTypeDefinition textFieldType() {
        return aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build();
    }

    private CaseFieldDefinition complexField(String id,
                                             String type,
                                             SecurityClassification securityClassification,
                                             String user,
                                             Boolean readAccess) {
        return newCaseField()
            .withId(id)
            .withFieldType(fieldType(type))
            .withSC(securityClassification.name())
            .withAcl(anAcl()
                .withRole(user)
                .withRead(readAccess)
                .build())
            .build();
    }

    private FieldTypeDefinition fieldType(String type) {
        return aFieldType()
            .withId(type)
            .withType(type)
            .build();
    }

}
