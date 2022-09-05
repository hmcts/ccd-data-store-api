package uk.gov.hmcts.ccd.domain.service.globalsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.search.global.Party;
import uk.gov.hmcts.ccd.domain.model.search.global.SearchCriteria;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationServiceImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.TEXT;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.AccessControlListBuilder.anAcl;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class GlobalSearchParserTest {

    protected static final ObjectMapper mapper = JacksonUtils.MAPPER;
    private static final String ROLE_IN_USER_ROLES = "caseworker-probate-loa1";
    private static final String ROLE_IN_USER_ROLES_2 = "caseworker-divorce-loa";
    private static final String ROLE_IN_USER_ROLE_1 = "Role 1";
    private static final Set<AccessProfile> USER_ACCESS_PROFILES = newHashSet(
        new AccessProfile(ROLE_IN_USER_ROLES),
        new AccessProfile(ROLE_IN_USER_ROLES_2),
        new AccessProfile(ROLE_IN_USER_ROLE_1));

    private static final String CASE_TYPE_ID_1 = "CASE_TYPE_1";
    private static final String CASE_TYPE_ID_2 = "CASE_TYPE_2";
    private static final String CASE_TYPE_ID_3 = "CASE_TYPE_3";
    private static final String JURISDICTION = "JURISDICTION";
    private static final String CASE_FIELD_2 = "Case field 2";
    private static final String CASE_FIELD_3 = "Case field 3";
    private static final String TEXT_TYPE = "Text";
    private CaseDetails caseDetails;
    private CaseDetails caseDetails2;
    private JurisdictionDefinition jurisdiction;
    private List<String> validFields;
    private List<Party> parties;
    private  CaseTypeDefinition caseTypeDefinition1;
    private  CaseTypeDefinition caseTypeDefinition2;
    private  CaseTypeDefinition caseTypeDefinition3;

    @Mock
    private CaseDataAccessControl caseDataAccessControl;
    @Mock
    private CaseTypeService caseTypeService;
    @Mock
    private SecurityClassificationServiceImpl securityClassificationService;

    private GlobalSearchParser globalSearchParser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validFields = List.of("ValidEntry", "ValidEntryTwo");
        Party party = new Party();
        party.setPartyName("name");
        parties = List.of(party);
        doReturn(USER_ACCESS_PROFILES).when(caseDataAccessControl).generateAccessProfilesByCaseDetails(any());

        jurisdiction = new JurisdictionDefinition();
        jurisdiction.setId(JURISDICTION);

        caseTypeDefinition1 = newCaseType()
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

        caseTypeDefinition2 = newCaseType()
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

        caseTypeDefinition3 = newCaseType()
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
            .withSecurityClassification(SecurityClassification.RESTRICTED)
            .withField(newCaseField().withId("searchCriteria")
                .withSC(SecurityClassification.PUBLIC.name())
                .withFieldType(
                    aFieldType()
                        .withId("searchCriteria")
                        .withType(COMPLEX)
                        .withComplexField(complexField("OtherCaseReferences", TEXT,
                            SecurityClassification.PUBLIC, ROLE_IN_USER_ROLE_1, true))
                        .withComplexField(complexField("SearchParties", TEXT,
                            SecurityClassification.PUBLIC, ROLE_IN_USER_ROLE_1, false))
                        .build())
                .build())
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .withField(newCaseField().withId(CASE_FIELD_3).withFieldType(textFieldType())
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLE_1)
                    .withRead(true)
                    .build()).build())
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .build();

        when(caseTypeService.getCaseType(CASE_TYPE_ID_1)).thenReturn(caseTypeDefinition1);
        when(caseTypeService.getCaseType(CASE_TYPE_ID_2)).thenReturn(caseTypeDefinition2);
        when(caseTypeService.getCaseType(CASE_TYPE_ID_3)).thenReturn(caseTypeDefinition3);

        globalSearchParser =
            new GlobalSearchParser(caseDataAccessControl, caseTypeService, securityClassificationService);
    }

    @Test
    void shouldNotFilterAnyResults() throws JsonProcessingException {
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setCaseManagementBaseLocationIds(validFields);
        searchCriteria.setCaseManagementRegionIds(validFields);
        doReturn(true).when(securityClassificationService)
            .userHasEnoughSecurityClassificationForField(any(), any());

        ObjectMapper mapper = new ObjectMapper();
        Map<String, JsonNode> data = new HashMap<>();
        data.put("caseManagementLocation", mapper.readTree("{\n"
            + "      \"region\": \"valueOne\",\n"
            + "       \"baseLocation\": \"valueThree\"\n"
            + "  }"));
        caseDetails = newCaseDetails()
            .withCaseTypeId(CASE_TYPE_ID_1)
            .withJurisdiction(JURISDICTION)
            .withData(data)
            .withSecurityClassification(SecurityClassification.PUBLIC).build();

        List<CaseDetails> response =
            globalSearchParser
                .filterCases(new java.util.ArrayList<>(java.util.Arrays.asList(caseDetails)), searchCriteria);

        assertAll(
            () -> assertThat(response.size(), Is.is(1))
        );

    }

    @Test
    void shouldFilterResultsWhenFieldDoesNotHaveReadPermission() throws JsonProcessingException {
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setCaseManagementBaseLocationIds(validFields);
        searchCriteria.setCaseManagementRegionIds(validFields);

        doReturn(true)
            .when(securityClassificationService).userHasEnoughSecurityClassificationForField(any(), any());

        ObjectMapper mapper = new ObjectMapper();
        Map<String, JsonNode> data = new HashMap<>();
        data.put("caseManagementLocation", mapper.readTree("{\n"
            + "      \"region\": \"valueOne\",\n"
            + "       \"baseLocation\": \"valueThree\"\n"
            + "  }"));
        caseDetails = newCaseDetails()
            .withCaseTypeId(CASE_TYPE_ID_1)
            .withJurisdiction(JURISDICTION)
            .withData(data)
            .withSecurityClassification(SecurityClassification.PUBLIC).build();

        caseDetails2 = newCaseDetails()
            .withCaseTypeId(CASE_TYPE_ID_2)
            .withJurisdiction(JURISDICTION)
            .withData(data)
            .withSecurityClassification(SecurityClassification.PUBLIC).build();

        List<CaseDetails> response =
            globalSearchParser
                .filterCases(new java.util.ArrayList<>(java.util.Arrays.asList(caseDetails, caseDetails2)),
                    searchCriteria);

        assertAll(
            () -> assertThat(response.size(), Is.is(1))
        );

    }

    @Test
    void shouldFilterResultsWhenFieldIsRestricted() throws JsonProcessingException {
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setCaseManagementBaseLocationIds(validFields);
        searchCriteria.setCaseManagementRegionIds(validFields);

        doReturn(false)
            .when(securityClassificationService)
            .userHasEnoughSecurityClassificationForField(any(CaseTypeDefinition.class),
                eq(SecurityClassification.RESTRICTED));
        doReturn(true)
            .when(securityClassificationService)
            .userHasEnoughSecurityClassificationForField(any(CaseTypeDefinition.class),
                eq(SecurityClassification.PUBLIC));
        doReturn(true)
            .when(securityClassificationService)
            .userHasEnoughSecurityClassificationForField(any(CaseTypeDefinition.class),
                eq(SecurityClassification.PUBLIC));
        doReturn(true)
            .when(securityClassificationService)
            .userHasEnoughSecurityClassificationForField(any(CaseTypeDefinition.class),
                eq(SecurityClassification.PUBLIC));


        ObjectMapper mapper = new ObjectMapper();
        Map<String, JsonNode> data = new HashMap<>();
        data.put("caseManagementLocation", mapper.readTree("{\n"
            + "      \"region\": \"valueOne\",\n"
            + "       \"baseLocation\": \"valueThree\"\n"
            + "  }"));
        caseDetails = newCaseDetails()
            .withCaseTypeId(CASE_TYPE_ID_1)
            .withJurisdiction(JURISDICTION)
            .withData(data)
            .withSecurityClassification(SecurityClassification.PUBLIC).build();

        caseDetails2 = newCaseDetails()
            .withCaseTypeId(CASE_TYPE_ID_3)
            .withJurisdiction(JURISDICTION)
            .withData(data)
            .withSecurityClassification(SecurityClassification.RESTRICTED).build();

        List<CaseDetails> response =
            globalSearchParser
                .filterCases(new java.util.ArrayList<>(java.util.Arrays.asList(caseDetails, caseDetails2)),
                    searchCriteria);

        assertAll(
            () -> assertThat(response.size(), Is.is(1))
        );

    }

    @Test
    void shouldFilterResultsWhenRequestIncludesPartiesAndOtherReferences() throws JsonProcessingException {
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setParties(parties);
        searchCriteria.setOtherReferences(validFields);

        doReturn(true).when(securityClassificationService)
            .userHasEnoughSecurityClassificationForField(any(), any());

        ObjectMapper mapper = new ObjectMapper();
        Map<String, JsonNode> data = new HashMap<>();
        data.put("searchCriteria", mapper.readTree("{\n"
            + "      \"OtherCaseReferences\": \"valueOne\",\n"
            + "       \"SearchParties\": \"valueThree\"\n"
            + "  }"));
        caseDetails = newCaseDetails()
            .withCaseTypeId(CASE_TYPE_ID_1)
            .withJurisdiction(JURISDICTION)
            .withData(data)
            .withSecurityClassification(SecurityClassification.PUBLIC).build();

        caseDetails2 = newCaseDetails()
            .withCaseTypeId(CASE_TYPE_ID_3)
            .withJurisdiction(JURISDICTION)
            .withData(data)
            .withSecurityClassification(SecurityClassification.PUBLIC).build();

        List<CaseDetails> response =
            globalSearchParser
                .filterCases(new java.util.ArrayList<>(java.util.Arrays.asList(caseDetails, caseDetails2)),
                    searchCriteria);

        assertAll(
            () -> assertThat(response.size(), Is.is(1))
        );

    }

    @Test
    void shouldFilterResultsWhenResponseIsEmpty() {
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setParties(parties);
        searchCriteria.setOtherReferences(validFields);

        List<CaseDetails> response =
            globalSearchParser.filterCases(Collections.emptyList(), searchCriteria);

        assertAll(
            () -> assertThat(response.size(), Is.is(0))
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
