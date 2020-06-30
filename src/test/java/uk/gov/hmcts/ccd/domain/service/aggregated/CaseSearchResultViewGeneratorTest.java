package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewHeader;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.HeaderGroupMetadata;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.CaseSearchResultView;
import uk.gov.hmcts.ccd.domain.service.common.*;
import uk.gov.hmcts.ccd.domain.service.processor.SearchResultProcessor;
import uk.gov.hmcts.ccd.domain.service.search.*;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;
import static uk.gov.hmcts.ccd.domain.service.aggregated.CaseDetailsUtil.CaseDetailsBuilder.caseDetails;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation.WORKBASKET;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.SearchResultBuilder.searchResult;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.buildData;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.buildSearchResultField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.AccessControlListBuilder.anAcl;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.ComplexACLBuilder.aComplexACL;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class CaseSearchResultViewGeneratorTest {

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


    private CaseSearchesViewAccessControl caseSearchesViewAccessControl;
    @Mock
    private SearchResultProcessor searchResultProcessor;

    private CaseSearchResultViewGenerator classUnderTest;

    private Map<String, JsonNode> dataMap;
    private JurisdictionDefinition jurisdiction;
    private CaseSearchResult caseSearchResult;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        caseSearchesViewAccessControl = new CaseSearchesViewAccessControl(userRepository,
            caseTypeService, searchResultDefinitionService, securityClassificationService);

        dataMap = buildData(CASE_FIELD_1, CASE_FIELD_2, CASE_FIELD_3, CASE_FIELD_4, CASE_FIELD_5);
        ObjectNode familyDetails = (ObjectNode) new ObjectMapper().readTree(FAMILY_DETAILS_VALUE);
        dataMap.put(FAMILY_DETAILS, familyDetails);

        CaseDetails caseDetails1 = caseDetails().withReference(999L)
            .withData(dataMap)
            .withCaseTypeId(CASE_TYPE_ID_1)
            .withJurisdiction(JURISDICTION)
            .withState("state1").withCreated(CREATED_DATE)
            .withLastModified(LAST_MODIFIED_DATE)
            .withLastStateModified(LAST_STATE_MODIFIED_DATE)
            .withSecurityClassification(SECURITY_CLASSIFICATION)
            .build();
        CaseDetails caseDetails2 = caseDetails()
            .withReference(1000L)
            .withData(dataMap)
            .withCaseTypeId(CASE_TYPE_ID_1)
            .withJurisdiction(JURISDICTION)
            .withState("state2")
            .withSecurityClassification(SECURITY_CLASSIFICATION)
            .build();
        CaseDetails caseDetails3 = caseDetails()
            .withReference(1001L)
            .withData(dataMap)
            .withCaseTypeId(CASE_TYPE_ID_2)
            .withJurisdiction(JURISDICTION)
            .withState("state2")
            .withSecurityClassification(SECURITY_CLASSIFICATION)
            .build();

        caseSearchResult = new CaseSearchResult(3L, Arrays.asList(caseDetails1, caseDetails2, caseDetails3));

        final CaseFieldDefinition fatherName = newCaseField().withId(FATHER_NAME)
            .withFieldType(textFieldType())
            .withSC(SECURITY_CLASSIFICATION.name())
            .withAcl(anAcl()
            .withRole(ROLE_IN_USER_ROLE_1)
            .withRead(true)
            .build()).build();
        final CaseFieldDefinition motherName = newCaseField().withId(MOTHER_NAME)
            .withFieldType(textFieldType())
            .withSC(SECURITY_CLASSIFICATION.name())
            .withAcl(anAcl()
            .withRole(ROLE_IN_USER_ROLE_1)
            .withRead(true)
            .build()).build();

        final CaseFieldDefinition addressLine1 = newCaseField().withId(ADDRESS_LINE_1)
            .withFieldType(textFieldType())
            .withSC(SECURITY_CLASSIFICATION.name())
            .withAcl(anAcl()
            .withRole(ROLE_IN_USER_ROLE_1)
            .withRead(true)
            .build()).build();
        final CaseFieldDefinition postCode = newCaseField().withId(POSTCODE)
            .withFieldType(textFieldType())
            .withSC(SECURITY_CLASSIFICATION.name())
            .withAcl(anAcl()
            .withRole(ROLE_IN_USER_ROLE_1)
            .withRead(true)
            .build()).build();
        final FieldTypeDefinition addressFieldTypeDefinition = aFieldType().withId(FAMILY_ADDRESS).withType(COMPLEX)
            .withComplexField(addressLine1).withComplexField(postCode).build();
        final CaseFieldDefinition familyAddress = newCaseField().withId(FAMILY_ADDRESS).withFieldType(addressFieldTypeDefinition).withAcl(anAcl()
            .withRole(ROLE_IN_USER_ROLE_1)
            .withRead(true)
            .build()).build();

        final FieldTypeDefinition familyDetailsFieldTypeDefinition =
            aFieldType().withId(FAMILY).withType(COMPLEX)
                .withComplexField(fatherName)
                .withComplexField(motherName)
                .withComplexField(familyAddress)
                .build();

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
            .withField(newCaseField().withId(FAMILY_DETAILS).withFieldType(familyDetailsFieldTypeDefinition)
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLE_1)
                    .withRead(true)
                    .build())
                .withComplexACL(aComplexACL()
                    .withListElementCode("Line1")
                    .withRole(ROLE_IN_USER_ROLE_1)
                    .withRead(true)
                    .withUpdate(false)
                    .build())
                .build())
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .build();

        CaseTypeDefinition caseTypeDefinition2 = newCaseType()
            .withCaseTypeId(CASE_TYPE_ID_2)
            .withJurisdiction(jurisdiction)
            .withField(newCaseField().withId(CASE_FIELD_4).withFieldType(textFieldType())
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLE_1)
                    .withRead(true)
                    .build()).build())
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .withField(newCaseField().withId(CASE_FIELD_5).withFieldType(textFieldType())
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLE_1)
                    .withRead(true)
                    .build()).build())
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .build();

        when(caseTypeService.getCaseType(eq(CASE_TYPE_ID_1))).thenReturn(caseTypeDefinition1);
        when(caseTypeService.getCaseType(eq(CASE_TYPE_ID_2))).thenReturn(caseTypeDefinition2);

        SearchResultDefinition caseType1SearchResult = searchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_1, "", CASE_FIELD_1, "", ""),
                buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_2, "", CASE_FIELD_2, "", ""))
            .build();
        SearchResultDefinition caseType2SearchResult = searchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID_2, CASE_FIELD_4, "", CASE_FIELD_4, "", ""))
            .build();
        when(searchResultDefinitionService.getSearchResultDefinition(any(), any(), any())).thenReturn(caseType1SearchResult, caseType2SearchResult);
        doAnswer(i -> i.getArgument(1)).when(searchResultProcessor).execute(any(), any());
        when(securityClassificationService.userHasEnoughSecurityClassificationForField(any(), any(), any())).thenReturn(true);

        classUnderTest = new CaseSearchResultViewGenerator(userRepository,
            caseTypeService, searchResultDefinitionService, searchResultProcessor, caseSearchesViewAccessControl);
    }

    @Test
    void shouldBuildCaseSearchResultHeaders() {
        when(userRepository.getUserRoles()).thenReturn(Sets.newHashSet(ROLE_IN_USER_ROLE_1, ROLE_IN_USER_ROLE_2));

        final CaseSearchResultView caseSearchResultView = classUnderTest.execute(CASE_TYPE_ID_1, caseSearchResult, WORKBASKET, Collections.emptyList());

        assertAll(
            () -> assertThat(caseSearchResultView.getHeaders().size(), is(1)),
            () -> assertMetadata(caseSearchResultView.getHeaders().get(0).getMetadata(), CASE_TYPE_ID_1, JURISDICTION),
            () -> assertCasesList(caseSearchResultView.getHeaders().get(0).getCases(), 2, "999", "1000"),
            () -> assertThat(caseSearchResultView.getHeaders().get(0).getFields().size(), is(2)),
            () -> assertHeaderField(caseSearchResultView.getHeaders().get(0).getFields().get(0), CASE_FIELD_1, CASE_FIELD_1, TEXT_TYPE)
        );
    }

    @Test
    void shouldBuildCaseSearchResultCases() {
        final CaseSearchResultView caseSearchResultView = classUnderTest.execute(CASE_TYPE_ID_1, caseSearchResult, "ORGCASES", Collections.emptyList());

        Map<String, Object> caseDataDifferences = Maps.difference(caseSearchResultView.getCases().get(0).getFields(),
            dataMap).entriesOnlyOnRight();
        assertAll(
            () -> assertThat(caseSearchResultView.getCases().size(), is(3)),
            () -> assertThat(caseSearchResultView.getCases().get(0).getCaseId(), is("999")),
            () -> assertThat(caseSearchResultView.getCases().get(1).getCaseId(), is("1000")),
            () -> assertThat(caseSearchResultView.getCases().get(2).getCaseId(), is("1001")),
            () -> assertThat(caseDataDifferences.isEmpty(), is(true)),
            () -> assertThat(Maps.difference(caseSearchResultView.getCases().get(0).getFieldsFormatted(),
                caseSearchResultView.getCases().get(0).getFields()).areEqual(), is(true)),
            () -> assertThat(caseSearchResultView.getCases().get(0).getFields().get("[JURISDICTION]"), is(JURISDICTION)),
            () -> assertThat(caseSearchResultView.getCases().get(0).getFields().get("[STATE]"), is("state1")),
            () -> assertThat(caseSearchResultView.getCases().get(0).getFields().get("[LAST_STATE_MODIFIED_DATE]"), is(LAST_STATE_MODIFIED_DATE)),
            () -> assertThat(caseSearchResultView.getCases().get(0).getFields().get("[CREATED_DATE]"), is(CREATED_DATE)),
            () -> assertThat(caseSearchResultView.getCases().get(0).getFields().get("[CASE_REFERENCE]"), is(999L)),
            () -> assertThat(caseSearchResultView.getCases().get(0).getFields().get("[SECURITY_CLASSIFICATION]"), is(SECURITY_CLASSIFICATION)),
            () -> assertThat(caseSearchResultView.getCases().get(0).getFields().get("[CASE_TYPE]"), is(CASE_TYPE_ID_1)),
            () -> assertThat(caseSearchResultView.getCases().get(0).getFields().get("[LAST_MODIFIED_DATE]"), is(LAST_MODIFIED_DATE))
        );
    }

    @Test
    void shouldBuildCaseSearchResultTotal() {
        final CaseSearchResultView searchResultView = classUnderTest.execute(CASE_TYPE_ID_1, caseSearchResult, WORKBASKET, Collections.emptyList());

        assertAll(
            () -> assertThat(searchResultView.getTotal(), is(3L))
        );
    }

    @Test
    void shouldBuildHeaderFieldsWithComplexFields() {
        SearchResultDefinition searchResult = searchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_1, "", CASE_FIELD_1, "", ""),
                buildSearchResultField(CASE_TYPE_ID_1, FAMILY_DETAILS, FATHER_NAME, FATHER_NAME, "", ""),
                buildSearchResultField(CASE_TYPE_ID_1, FAMILY_DETAILS, MOTHER_NAME, MOTHER_NAME, "", ""))
            .build();
        when(searchResultDefinitionService.getSearchResultDefinition(any(), any(), any())).thenReturn(searchResult);
        when(userRepository.getUserRoles()).thenReturn(Sets.newHashSet(ROLE_IN_USER_ROLE_1, ROLE_IN_USER_ROLE_2));

        final CaseSearchResultView caseSearchResultView = classUnderTest.execute(CASE_TYPE_ID_1, caseSearchResult, WORKBASKET, Collections.emptyList());

        assertAll(
            () -> assertThat(caseSearchResultView.getHeaders().size(), is(1)),
            () -> assertThat(caseSearchResultView.getHeaders().get(0).getFields().size(), is(3)),
            () -> assertHeaderField(caseSearchResultView.getHeaders().get(0).getFields().get(0),
                CASE_FIELD_1, CASE_FIELD_1, TEXT_TYPE),
            () -> assertHeaderField(caseSearchResultView.getHeaders().get(0).getFields().get(1),
                FAMILY_DETAILS + SEPARATOR + FATHER_NAME, FATHER_NAME, TEXT_TYPE),
            () -> assertHeaderField(caseSearchResultView.getHeaders().get(0).getFields().get(2),
                FAMILY_DETAILS + SEPARATOR + MOTHER_NAME, MOTHER_NAME, TEXT_TYPE)
        );
    }

    @Test
    void shouldBuildHeaderFieldsForPermittedRoles() {
        CaseTypeDefinition caseTypeDefinition = newCaseType()
            .withCaseTypeId(CASE_TYPE_ID_1)
            .withJurisdiction(jurisdiction)
            .withField(newCaseField().withId(CASE_FIELD_1).withFieldType(textFieldType()).withAcl(anAcl()
                .withRole(ROLE_IN_USER_ROLE_1)
                .withRead(true)
                .build()).build())
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .withField(newCaseField().withId(CASE_FIELD_2).withFieldType(textFieldType()).withAcl(anAcl()
                .withRole(ROLE_IN_USER_ROLE_1)
                .withRead(true)
                .build()).build())
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .withField(newCaseField().withId(CASE_FIELD_4).withFieldType(textFieldType()).withAcl(anAcl()
                .withRole(ROLE_IN_USER_ROLE_1)
                .withRead(true)
                .build()).build())
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .withField(newCaseField().withId(CASE_FIELD_5).withFieldType(textFieldType()).withAcl(anAcl()
                .withRole(ROLE_IN_USER_ROLE_1)
                .withRead(true)
                .build()).build())
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .build();
        SearchResultField searchResultFieldWithValidRole = buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_4, "", CASE_FIELD_4, "", "");
        searchResultFieldWithValidRole.setRole(ROLE_IN_USER_ROLE_1);
        SearchResultField searchResultFieldWithInvalidRole = buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_5, "", CASE_FIELD_5, "", "");
        searchResultFieldWithInvalidRole.setRole(ROLE_NOT_IN_USER_ROLE);
        SearchResultDefinition searchResult = searchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_1, "", CASE_FIELD_1, "", ""),
                buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_2, "", CASE_FIELD_2, "", ""),
                searchResultFieldWithValidRole,
                searchResultFieldWithInvalidRole)
            .build();
        when(caseTypeService.getCaseType(eq(CASE_TYPE_ID_1))).thenReturn(caseTypeDefinition);
        when(searchResultDefinitionService.getSearchResultDefinition(any(), any(), any())).thenReturn(searchResult);
        when(userRepository.getUserRoles()).thenReturn(Sets.newHashSet(ROLE_IN_USER_ROLE_1));

        final CaseSearchResultView caseSearchResultView = classUnderTest.execute(CASE_TYPE_ID_1, caseSearchResult, WORKBASKET, Collections.emptyList());

        assertAll(
            () -> assertThat(caseSearchResultView.getHeaders().get(0).getFields().size(), is(3)),
            () -> assertThat(caseSearchResultView.getHeaders().get(0).getFields().get(0).getCaseFieldId(), is(CASE_FIELD_1)),
            () -> assertThat(caseSearchResultView.getHeaders().get(0).getFields().get(1).getCaseFieldId(), is(CASE_FIELD_2)),
            () -> assertThat(caseSearchResultView.getHeaders().get(0).getFields().get(2).getCaseFieldId(), is(CASE_FIELD_4))
        );
    }

    @Test
    void shouldBuildHeaderFieldsWithNoDuplicateColumnsForMultiplePermittedRoles() {
        SearchResultField searchResultFieldWithValidRole = buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_4, "", CASE_FIELD_4, "", "");
        searchResultFieldWithValidRole.setRole(ROLE_IN_USER_ROLE_1);
        SearchResultField searchResultFieldWithValidRole2 = buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_4, "", CASE_FIELD_4, "", "");
        searchResultFieldWithValidRole2.setRole(ROLE_IN_USER_ROLE_2);
        SearchResultField searchResultFieldWithInvalidRole = buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_5, "", CASE_FIELD_5, "", "");
        searchResultFieldWithInvalidRole.setRole(ROLE_NOT_IN_USER_ROLE);
        SearchResultDefinition searchResult = searchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_1, "", CASE_FIELD_1, "", ""),
                buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_1, "", CASE_FIELD_1, "", ""),
                buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_1, "", CASE_FIELD_1, "", ""),
                buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_2, "", CASE_FIELD_2, "", ""),
                buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_2, "", CASE_FIELD_2, "", ""),
                searchResultFieldWithValidRole,
                searchResultFieldWithValidRole2,
                searchResultFieldWithInvalidRole)
            .build();
        CaseTypeDefinition caseTypeDefinition = newCaseType()
            .withCaseTypeId(CASE_TYPE_ID_1)
            .withJurisdiction(jurisdiction)
            .withField(newCaseField().withId(CASE_FIELD_1).withFieldType(textFieldType())
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLE_1)
                    .withRead(true)
                    .build()).build())
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .withField(newCaseField().withId(CASE_FIELD_2).withFieldType(textFieldType()).withAcl(anAcl()
                .withRole(ROLE_IN_USER_ROLE_1)
                .withRead(true)
                .build()).build())
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .withField(newCaseField().withId(CASE_FIELD_4).withFieldType(textFieldType()).withAcl(anAcl()
                .withRole(ROLE_IN_USER_ROLE_1)
                .withRead(true)
                .build()).build())
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .withField(newCaseField().withId(CASE_FIELD_5).withFieldType(textFieldType()).withAcl(anAcl()
                .withRole(ROLE_IN_USER_ROLE_1)
                .withRead(true)
                .build()).build())
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .build();
        when(caseTypeService.getCaseType(eq(CASE_TYPE_ID_1))).thenReturn(caseTypeDefinition);
        when(searchResultDefinitionService.getSearchResultDefinition(any(), any(), any())).thenReturn(searchResult);
        when(userRepository.getUserRoles()).thenReturn(Sets.newHashSet(ROLE_IN_USER_ROLE_1, ROLE_IN_USER_ROLE_2));

        final CaseSearchResultView caseSearchResultView = classUnderTest.execute(CASE_TYPE_ID_1, caseSearchResult, WORKBASKET, Collections.emptyList());

        assertAll(
            () -> assertThat(caseSearchResultView.getHeaders().get(0).getFields().size(), is(3)),
            () -> assertThat(caseSearchResultView.getHeaders().get(0).getFields().get(0).getCaseFieldId(), is(CASE_FIELD_1)),
            () -> assertThat(caseSearchResultView.getHeaders().get(0).getFields().get(1).getCaseFieldId(), is(CASE_FIELD_2)),
            () -> assertThat(caseSearchResultView.getHeaders().get(0).getFields().get(2).getCaseFieldId(), is(CASE_FIELD_4))
        );
    }

    @Test
    void shouldBuildResultsWithComplexNestedElements() {
        SearchResultField searchResultFieldWithValidRole =
            buildSearchResultField(CASE_TYPE_ID_1, FAMILY_DETAILS, FAMILY_DETAILS_PATH, FAMILY_DETAILS, "", "");
        searchResultFieldWithValidRole.setRole(ROLE_IN_USER_ROLE_1);
        SearchResultField searchResultFieldWithValidRoleNested =
            buildSearchResultField(CASE_TYPE_ID_1, FAMILY_DETAILS, FAMILY_DETAILS_PATH_NESTED, FAMILY_DETAILS, "", "");
        searchResultFieldWithValidRoleNested.setRole(ROLE_IN_USER_ROLE_1);

        SearchResultDefinition searchResult = searchResult()
            .withSearchResultFields(
                searchResultFieldWithValidRole,
                searchResultFieldWithValidRoleNested)
            .build();

        when(searchResultDefinitionService.getSearchResultDefinition(any(), any(), any())).thenReturn(searchResult);
        when(userRepository.getUserRoles()).thenReturn(Sets.newHashSet(ROLE_IN_USER_ROLE_1, ROLE_IN_USER_ROLE_2));

        CaseSearchResultView caseSearchResultView = classUnderTest.execute(CASE_TYPE_ID_1, caseSearchResult, WORKBASKET, Collections.emptyList());

        assertAll(
            () -> assertHeaderField(caseSearchResultView.getHeaders().get(0).getFields().get(0),
                FAMILY + SEPARATOR + FATHER_NAME, FAMILY_DETAILS, TEXT_TYPE),
            () -> assertHeaderField(caseSearchResultView.getHeaders().get(0).getFields().get(1),
                FAMILY + SEPARATOR + FAMILY_DETAILS_PATH_NESTED, FAMILY_DETAILS, TEXT_TYPE),
            () -> assertThat(((TextNode) caseSearchResultView.getCases().get(0).getFields()
                .get(FAMILY + SEPARATOR + FATHER_NAME)).asText(), is(FATHER_NAME_VALUE)),
            () -> assertThat(((TextNode) caseSearchResultView.getCases().get(0).getFields()
                .get(FAMILY + SEPARATOR + FAMILY_DETAILS_PATH_NESTED)).asText(), is(POSTCODE_VALUE))
        );
    }

    @Test
    void shouldInvokeSearchProcessorDCPIsProvided() {
        SearchResultDefinition searchResult = searchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_1, "", CASE_FIELD_1, "#DCP", ""))
            .build();
        when(searchResultDefinitionService.getSearchResultDefinition(any(), any(), any())).thenReturn(searchResult);
        when(userRepository.getUserRoles()).thenReturn(Sets.newHashSet(ROLE_IN_USER_ROLE_1, ROLE_IN_USER_ROLE_2));

        classUnderTest.execute(CASE_TYPE_ID_1, caseSearchResult, WORKBASKET, Collections.emptyList());

        verify(searchResultProcessor).execute(any(), any());
    }

    @Test
    void shouldNotInvokeSearchProcessorWhenNoDCPProvided() {
        SearchResultDefinition searchResult = searchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_1, "", CASE_FIELD_1, null, ""))
            .build();
        when(searchResultDefinitionService.getSearchResultDefinition(any(), any(), any())).thenReturn(searchResult);

        classUnderTest.execute(CASE_TYPE_ID_1, caseSearchResult, WORKBASKET, Collections.emptyList());

        verifyNoMoreInteractions(searchResultProcessor);
    }

    @Test
    void shouldThrowBadRequestExceptionWhenNoNestedElementFoundForPath() {
        SearchResultDefinition searchResult = searchResult()
            .withSearchResultFields(buildSearchResultField(CASE_TYPE_ID_1,
                FAMILY_DETAILS, "InvalidElement",
                FAMILY_DETAILS, "", ""))
            .build();
        when(searchResultDefinitionService.getSearchResultDefinition(any(), any(), any())).thenReturn(searchResult);
        when(userRepository.getUserRoles()).thenReturn(Sets.newHashSet(ROLE_IN_USER_ROLE_1, ROLE_IN_USER_ROLE_2));

        final BadRequestException exception = assertThrows(BadRequestException.class,
            () -> classUnderTest.execute(CASE_TYPE_ID_1, caseSearchResult, WORKBASKET, Collections.emptyList()));

        assertAll(
            () -> assertThat(exception.getMessage(), is("CaseField FamilyDetails has no nested elements with code InvalidElement."))
        );
    }

    @Test
    void shouldNotNotReturnHeaderFieldsWhenNoNestedElementFoundForPath() {
        SearchResultDefinition searchResult = searchResult()
            .withSearchResultFields(buildSearchResultField(CASE_TYPE_ID_1,
                FAMILY_DETAILS, "InvalidElementPath",
                FAMILY_DETAILS, "", ""))
            .build();
        CaseTypeDefinition caseTypeWithoutCaseFieldDefinition = newCaseType().withCaseTypeId(CASE_TYPE_ID_1).withJurisdiction(jurisdiction)
            .withField(newCaseField().withId(CASE_FIELD_1).withFieldType(textFieldType()).withAcl(anAcl()
                .withRole(ROLE_IN_USER_ROLE_1)
                .withRead(true)
                .build()).build())
            .withSecurityClassification(SecurityClassification.PUBLIC).build();
        when(searchResultDefinitionService.getSearchResultDefinition(any(), any(), any())).thenReturn(searchResult);
        when(caseTypeService.getCaseType(eq(CASE_TYPE_ID_1))).thenReturn(caseTypeWithoutCaseFieldDefinition);

        final CaseSearchResultView caseSearchResultView = classUnderTest.execute(CASE_TYPE_ID_1,
            caseSearchResult, WORKBASKET, Collections.emptyList());

        assertAll(
            () -> assertThat(caseSearchResultView.getCases().size(), is(3)),
            () -> assertThat(caseSearchResultView.getHeaders().get(0).getFields().size(), is(0)),
            () -> assertNull(caseSearchResultView.getCases().get(0).getFields().get("InvalidElementPath")),
            () -> assertNull(caseSearchResultView.getCases().get(1).getFields().get("InvalidElementPath")),
            () -> assertNull(caseSearchResultView.getCases().get(2).getFields().get("InvalidElementPath")));
    }

    @Test
    void shouldThrowBadSearchRequestExceptionWhenUseCaseDoesNotExist() {
        SearchResultDefinition searchResult = searchResult().withSearchResultFields().build();
        when(searchResultDefinitionService.getSearchResultDefinition(any(), any(), any())).thenReturn(searchResult);

        final BadSearchRequest exception = assertThrows(BadSearchRequest.class, () ->
            classUnderTest.execute(CASE_TYPE_ID_1, caseSearchResult, "INVALID", Collections.emptyList()));

        assertAll(
            () -> assertThat(exception.getMessage(), is("The provided use case 'INVALID' is unsupported for case type 'CASE_TYPE_1'."))
        );
    }

    private void assertMetadata(HeaderGroupMetadata metadata, String caseTypeId, String jurisdiction) {
        assertAll(
            () -> assertThat(metadata.getJurisdiction(), is(jurisdiction)),
            () -> assertThat(metadata.getCaseTypeId(), is(caseTypeId))
        );
    }

    private void assertCasesList(List<String> actualValues, int expectedSize, String... expectedValues) {
        assertAll(
            () -> assertThat(actualValues.size(), is(expectedSize)),
            () -> IntStream.range(0, actualValues.size()).forEach(idx -> assertThat(actualValues.get(idx), is(expectedValues[idx])))
        );
    }

    private void assertHeaderField(SearchResultViewHeader field, String caseFieldId, String label, String fieldType) {
        assertAll(
            () -> assertThat(field.getCaseFieldId(), is(caseFieldId)),
            () -> assertThat(field.getLabel(), is(label)),
            () -> assertThat(field.getCaseFieldTypeDefinition().getType(), is(fieldType))
        );
    }

    private FieldTypeDefinition textFieldType() {
        return aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build();
    }
}
