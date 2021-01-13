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
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResultDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;
import uk.gov.hmcts.ccd.domain.service.processor.date.DateTimeSearchResultProcessor;
import uk.gov.hmcts.ccd.domain.service.search.CaseSearchesViewAccessControl;
import uk.gov.hmcts.ccd.domain.service.search.SearchResultDefinitionService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.SearchResultBuilder.searchResult;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.buildData;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.buildSearchResultField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.AccessControlListBuilder.anAcl;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class CaseSearchesViewAccessControlTest {

    private static final String CASE_TYPE_ID_1 = "CASE_TYPE_1";
    private static final String CASE_TYPE_ID_2 = "CASE_TYPE_2";
    private static final String JURISDICTION = "JURISDICTION";
    private static final String CASE_FIELD_1 = "Case field 1";
    private static final String CASE_FIELD_2 = "Case field 2";
    private static final String CASE_FIELD_3 = "Case field 3";
    private static final String CASE_FIELD_4 = "Case field 4";
    private static final String CASE_FIELD_5 = "Case field 5";
    private static final String ROLE_IN_USER_ROLE_1 = "Role 1";
    private static final String ROLE_IN_USER_ROLE_2 = "Role 2";
    private static final String ROLE_NOT_IN_USER_ROLE = "Role X";

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
    private static final String FAMILY_DETAILS_PATH = "FatherName";
    private static final String FAMILY_DETAILS_PATH_NESTED = "FamilyAddress.PostCode";
    private static final String FAMILY = "FamilyDetails";
    private static final String FATHER_NAME = "FatherName";
    private static final String MOTHER_NAME = "MotherName";
    private static final String FAMILY_ADDRESS = "FamilyAddress";
    private static final String ADDRESS_LINE_1 = "AddressLine1";
    private static final String POSTCODE = "PostCode";

    private static final String TEXT_TYPE = "Text";
    private static final String SEPARATOR = ".";

    private static final LocalDateTime CREATED_DATE = LocalDateTime.of(2000, 1, 2, 12, 0);
    private static final LocalDateTime LAST_MODIFIED_DATE = LocalDateTime.of(1987, 12, 4, 17, 30);
    private static final LocalDateTime LAST_STATE_MODIFIED_DATE = LocalDateTime.of(2015, 6, 17, 20, 45);
    private static final SecurityClassification SECURITY_CLASSIFICATION = SecurityClassification.PUBLIC;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CaseTypeService caseTypeService;

    @Mock
    private SearchResultDefinitionService searchResultDefinitionService;

    @Mock
    private SecurityClassificationService securityClassificationService;

    @Mock
    private DateTimeSearchResultProcessor dateTimeSearchResultProcessor;

    private CaseSearchesViewAccessControl classUnderTest;

    private Map<String, JsonNode> dataMap;
    private JurisdictionDefinition jurisdiction;
    private static Long CASE_REFERENCE = 1234567890123456L;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);

        dataMap = buildData(CASE_FIELD_1, CASE_FIELD_2, CASE_FIELD_3, CASE_FIELD_4, CASE_FIELD_5);
        ObjectNode familyDetails = (ObjectNode) new ObjectMapper().readTree(FAMILY_DETAILS_VALUE);
        dataMap.put(FAMILY_DETAILS, familyDetails);

        jurisdiction = new JurisdictionDefinition();
        jurisdiction.setId(JURISDICTION);
        CaseTypeDefinition caseTypeDefinition1 = newCaseType()
            .withCaseTypeId(CASE_TYPE_ID_1)
            .withJurisdiction(jurisdiction)
            .withField(newCaseField().withId(CASE_FIELD_1).withFieldType(textFieldType())
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLE_1)
                    .withRead(true)
                    .build()).build())
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

        classUnderTest = new CaseSearchesViewAccessControl(userRepository, caseTypeService,
            searchResultDefinitionService, securityClassificationService);

    }

    @Test
    void shouldReturnTrueForFilterResultsBySearchResultsDefinition() {
        CaseTypeDefinition caseTypeDefinition1 = newCaseType()
            .withCaseTypeId(CASE_TYPE_ID_1)
            .withJurisdiction(jurisdiction)
            .withField(newCaseField().withId(CASE_FIELD_1).withFieldType(textFieldType())
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLE_1)
                    .withRead(true)
                    .build()).build())
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

        when(userRepository.getUserRoles()).thenReturn(Sets.newHashSet(ROLE_IN_USER_ROLE_1, ROLE_IN_USER_ROLE_2));
        List<String> requestedFields = new ArrayList<>();

        assertTrue(classUnderTest
            .filterResultsBySearchResultsDefinition("ORGCASES", caseTypeDefinition1, requestedFields, CASE_FIELD_1));
    }

    @Test
    void shouldReturnTrueForFilterResultsBySearchResultsDefinitionWhenUseCaseIsNull() {
        CaseTypeDefinition caseTypeDefinition1 = newCaseType()
            .withCaseTypeId(CASE_TYPE_ID_1)
            .withJurisdiction(jurisdiction)
            .withField(newCaseField().withId(CASE_FIELD_1).withFieldType(textFieldType())
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLE_1)
                    .withRead(true)
                    .build()).build())
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

        when(userRepository.getUserRoles()).thenReturn(Sets.newHashSet(ROLE_IN_USER_ROLE_1, ROLE_IN_USER_ROLE_2));
        List<String> requestedFields = new ArrayList<>();

        assertTrue(classUnderTest
            .filterResultsBySearchResultsDefinition(null, caseTypeDefinition1, requestedFields, CASE_FIELD_1));
    }

    @Test
    void shouldReturnFalseForFilterResultsBySearchResultsDefinition() {
        CaseTypeDefinition caseTypeDefinition1 = newCaseType()
            .withCaseTypeId(CASE_TYPE_ID_1)
            .withJurisdiction(jurisdiction)
            .withField(newCaseField().withId(CASE_FIELD_1).withFieldType(textFieldType())
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLE_1)
                    .withRead(true)
                    .build()).build())
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

        when(userRepository.getUserRoles()).thenReturn(Sets.newHashSet(ROLE_IN_USER_ROLE_1, ROLE_IN_USER_ROLE_2));
        List<String> requestedFields = new ArrayList<>();


        assertFalse(classUnderTest
            .filterResultsBySearchResultsDefinition("ORGCASES", caseTypeDefinition1, requestedFields, CASE_FIELD_2));
    }

    @Test
    void shouldReturnTrueForFilterFieldByAuthorisationAccessOnField() {
        final CaseFieldDefinition postCode = newCaseField().withId(POSTCODE)
            .withFieldType(textFieldType())
            .withSC(SECURITY_CLASSIFICATION.name())
            .withAcl(anAcl()
                .withRole(ROLE_IN_USER_ROLE_1)
                .withRead(true)
                .build()).build();
        when(userRepository.getUserRoles()).thenReturn(Sets.newHashSet(ROLE_IN_USER_ROLE_1, ROLE_IN_USER_ROLE_2));

        assertTrue(classUnderTest.filterFieldByAuthorisationAccessOnField(postCode));
    }

    @Test
    void shouldReturnFalseForFilterFieldByAuthorisationAccessOnField() {
        final CaseFieldDefinition postCode = newCaseField().withId(POSTCODE)
            .withFieldType(textFieldType())
            .withSC(SECURITY_CLASSIFICATION.name())
            .withAcl(anAcl()
                .withRole(ROLE_IN_USER_ROLE_1)
                .withRead(true)
                .build()).build();
        when(userRepository.getUserRoles()).thenReturn(Sets.newHashSet(ROLE_IN_USER_ROLE_2));

        assertFalse(classUnderTest.filterFieldByAuthorisationAccessOnField(postCode));
    }

    @Test
    void shouldReturnTrueForFilterResultsBySecurityClassification() {
        final CaseFieldDefinition caseFieldDefinition1 = newCaseField().withId(CASE_FIELD_1)
            .withFieldType(textFieldType())
            .withSC(SecurityClassification.PRIVATE.name())
            .withAcl(anAcl()
                .withRole(ROLE_IN_USER_ROLE_1)
                .withRead(true)
                .build()).build();

        CaseTypeDefinition caseTypeDefinition1 = newCaseType()
            .withCaseTypeId(CASE_TYPE_ID_1)
            .withJurisdiction(jurisdiction)
            .withField(newCaseField().withId(CASE_FIELD_1).withFieldType(textFieldType())
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLE_1)
                    .withRead(true)
                    .build()).build())
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


        when(userRepository.getUserRoles()).thenReturn(Sets.newHashSet(ROLE_IN_USER_ROLE_1, ROLE_IN_USER_ROLE_2));

        assertTrue(classUnderTest.filterResultsBySecurityClassification(caseFieldDefinition1, caseTypeDefinition1));
    }

    @Test
    void shouldReturnFalseForFilterResultsBySecurityClassification() {
        final CaseFieldDefinition caseFieldDefinition1 = newCaseField().withId(CASE_FIELD_1)
            .withFieldType(textFieldType())
            .withSC(SecurityClassification.PUBLIC.name())
            .withAcl(anAcl()
                .withRole(ROLE_IN_USER_ROLE_1)
                .withRead(true)
                .build()).build();

        CaseTypeDefinition caseTypeDefinition1 = newCaseType()
            .withCaseTypeId(CASE_TYPE_ID_1)
            .withJurisdiction(jurisdiction)
            .withField(newCaseField().withId(CASE_FIELD_1).withFieldType(textFieldType())
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLE_1)
                    .withRead(true)
                    .build()).build())
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

        when(userRepository.getUserRoles()).thenReturn(Sets.newHashSet(ROLE_IN_USER_ROLE_2));
        when(securityClassificationService.userHasEnoughSecurityClassificationForField(any(), any(), any()))
            .thenReturn(false);
        assertFalse(classUnderTest.filterResultsBySecurityClassification(caseFieldDefinition1, caseTypeDefinition1));
    }


    private FieldTypeDefinition textFieldType() {
        return aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build();
    }
}
