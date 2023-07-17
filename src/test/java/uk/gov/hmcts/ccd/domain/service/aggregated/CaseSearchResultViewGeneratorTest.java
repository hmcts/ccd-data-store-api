package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProcess;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.CaseAccessMetadata;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResultDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResultField;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.CaseSearchResultView;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.HeaderGroupMetadata;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewHeader;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationServiceImpl;
import uk.gov.hmcts.ccd.domain.service.processor.date.DateTimeSearchResultProcessor;
import uk.gov.hmcts.ccd.domain.service.search.CaseSearchResultViewGenerator;
import uk.gov.hmcts.ccd.domain.service.search.CaseSearchesViewAccessControl;
import uk.gov.hmcts.ccd.domain.service.search.SearchResultDefinitionService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;
import static uk.gov.hmcts.ccd.domain.service.aggregated.CaseDetailsUtil.CaseDetailsBuilder.caseDetails;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation.WORKBASKET;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.SearchResultBuilder.searchResult;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.buildData;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.buildSearchResultField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.AccessControlListBuilder.anAcl;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.ComplexACLBuilder.aComplexACL;

class CaseSearchResultViewGeneratorTest {

    private static final String CASE_TYPE_ID_1 = "CASE_TYPE_1";
    private static final String CASE_TYPE_ID_2 = "CASE_TYPE_2";
    private static final String JURISDICTION = "JURISDICTION";
    private static final String CASE_FIELD_1 = "Case field 1";
    private static final String CASE_FIELD_2 = "Case field 2";
    private static final String CASE_FIELD_3 = "Case field 3";
    private static final String CASE_FIELD_4 = "Case field 4";
    private static final String CASE_FIELD_5 = "Case field 5";
    private static final String SUPPLEMENTARY_DATA_FIELD_1 = "Supplementary data field 1";
    private static final String SUPPLEMENTARY_DATA_FIELD_2 = "Supplementary data field 2";
    private static final String ROLE_IN_USER_ROLE_1 = "Role 1";
    private static final String ROLE_IN_USER_ROLE_2 = "Role 2";
    private static final String ROLE_NOT_IN_USER_ROLE = "Role X";
    private static final Set<AccessProfile> ACCESS_PROFILES = createAccessProfiles(Sets
        .newHashSet(ROLE_IN_USER_ROLE_1, ROLE_IN_USER_ROLE_2));

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
    private CaseDataAccessControl caseDataAccessControl;

    @Mock
    private CaseTypeService caseTypeService;

    @Mock
    private SearchResultDefinitionService searchResultDefinitionService;

    @Mock
    private SecurityClassificationServiceImpl securityClassificationService;


    private CaseSearchesViewAccessControl caseSearchesViewAccessControl;
    @Mock
    private DateTimeSearchResultProcessor dateTimeSearchResultProcessor;

    private CaseSearchResultViewGenerator classUnderTest;

    private Map<String, JsonNode> dataMap;
    private Map<String, JsonNode> supplementaryDataMap;
    private JurisdictionDefinition jurisdiction;
    private CaseSearchResult caseSearchResult;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        caseSearchesViewAccessControl = new CaseSearchesViewAccessControl(caseTypeService,
            searchResultDefinitionService, securityClassificationService, caseDataAccessControl);

        dataMap = buildData(CASE_FIELD_1, CASE_FIELD_2, CASE_FIELD_3, CASE_FIELD_4, CASE_FIELD_5);
        supplementaryDataMap = buildData(SUPPLEMENTARY_DATA_FIELD_1, SUPPLEMENTARY_DATA_FIELD_2);
        ObjectNode familyDetails = (ObjectNode) new ObjectMapper().readTree(FAMILY_DETAILS_VALUE);
        dataMap.put(FAMILY_DETAILS, familyDetails);

        when(caseDataAccessControl.generateAccessProfilesByCaseTypeId(anyString()))
            .thenReturn(ACCESS_PROFILES);

        CaseDetails caseDetails1 = caseDetails().withReference(999L)
            .withData(dataMap)
            .withCaseTypeId(CASE_TYPE_ID_1)
            .withJurisdiction(JURISDICTION)
            .withState("state1").withCreated(CREATED_DATE)
            .withLastModified(LAST_MODIFIED_DATE)
            .withLastStateModified(LAST_STATE_MODIFIED_DATE)
            .withSecurityClassification(SECURITY_CLASSIFICATION)
            .withSupplementaryData(supplementaryDataMap)
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

        final CaseFieldDefinition fatherName = CaseFieldDefinition.builder().id(FATHER_NAME)
            .fieldTypeDefinition(textFieldType())
            .caseTypeId(CASE_TYPE_ID_1)
            .securityLabel(SECURITY_CLASSIFICATION.name())
            .accessControlLists(List.of(anAcl()
                .withRole(ROLE_IN_USER_ROLE_1)
                .withRead(true)
                .build())).build();
        final CaseFieldDefinition motherName = CaseFieldDefinition.builder().id(MOTHER_NAME)
            .fieldTypeDefinition(textFieldType())
            .caseTypeId(CASE_TYPE_ID_1)
            .securityLabel(SECURITY_CLASSIFICATION.name())
            .accessControlLists(List.of(anAcl()
                .withRole(ROLE_IN_USER_ROLE_1)
                .withRead(true)
                .build())).build();

        final CaseFieldDefinition addressLine1 = CaseFieldDefinition.builder().id(ADDRESS_LINE_1)
            .fieldTypeDefinition(textFieldType())
            .caseTypeId(CASE_TYPE_ID_1)
            .securityLabel(SECURITY_CLASSIFICATION.name())
            .accessControlLists(List.of(anAcl()
                .withRole(ROLE_IN_USER_ROLE_1)
                .withRead(true)
                .build())).build();
        final CaseFieldDefinition postCode = CaseFieldDefinition.builder().id(POSTCODE)
            .fieldTypeDefinition(textFieldType())
            .caseTypeId(CASE_TYPE_ID_1)
            .securityLabel(SECURITY_CLASSIFICATION.name())
            .accessControlLists(List.of(anAcl()
                .withRole(ROLE_IN_USER_ROLE_1)
                .withRead(true)
                .build())).build();
        final FieldTypeDefinition addressFieldTypeDefinition = FieldTypeDefinition.builder().id(FAMILY_ADDRESS)
            .type(COMPLEX)
            .complexFields(List.of(addressLine1, postCode)).build();
        final CaseFieldDefinition familyAddress =
            CaseFieldDefinition.builder().id(FAMILY_ADDRESS).fieldTypeDefinition(addressFieldTypeDefinition)
                .caseTypeId(CASE_TYPE_ID_1)
                .accessControlLists(List.of(anAcl()
            .withRole(ROLE_IN_USER_ROLE_1)
            .withRead(true)
            .build())).build();

        final FieldTypeDefinition familyDetailsFieldTypeDefinition =
            FieldTypeDefinition.builder().id(FAMILY).type(COMPLEX)
                .complexFields(List.of(fatherName, motherName, familyAddress))
                .build();

        jurisdiction = JurisdictionDefinition.builder().id(JURISDICTION).build();
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
                        .build())).build(),
                CaseFieldDefinition.builder().id(FAMILY_DETAILS).fieldTypeDefinition(familyDetailsFieldTypeDefinition)
                    .caseTypeId(CASE_TYPE_ID_1)
                    .accessControlLists(List.of(anAcl()
                        .withRole(ROLE_IN_USER_ROLE_1)
                        .withRead(true)
                        .build()))
                    .complexACLs(List.of(aComplexACL()
                        .withListElementCode("Line1")
                        .withRole(ROLE_IN_USER_ROLE_1)
                        .withRead(true)
                        .withUpdate(false)
                        .build()))
                    .build()
            ))
            .build();


        CaseTypeDefinition caseTypeDefinition2 = CaseTypeDefinition.builder()
            .id(CASE_TYPE_ID_2)
            .jurisdictionDefinition(jurisdiction)
            .securityClassification(SecurityClassification.PUBLIC)
            .caseFieldDefinitions(List.of(
                CaseFieldDefinition.builder().id(CASE_FIELD_4).fieldTypeDefinition(textFieldType())
                    .caseTypeId(CASE_TYPE_ID_2)
                    .accessControlLists(List.of(anAcl()
                        .withRole(ROLE_IN_USER_ROLE_1)
                        .withRead(true)
                        .build())).build(),
                CaseFieldDefinition.builder().id(CASE_FIELD_5).fieldTypeDefinition(textFieldType())
                    .caseTypeId(CASE_TYPE_ID_2)
                    .accessControlLists(List.of(anAcl()
                        .withRole(ROLE_IN_USER_ROLE_1)
                        .withRead(true)
                        .build())).build()
            ))
            .build();

        when(caseTypeService.getCaseType(eq(CASE_TYPE_ID_1))).thenReturn(caseTypeDefinition1);
        when(caseTypeService.getCaseType(eq(CASE_TYPE_ID_2))).thenReturn(caseTypeDefinition2);

        SearchResultDefinition caseType1SearchResult = searchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_1, "", CASE_FIELD_1,
                    "", ""),
                buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_2, "", CASE_FIELD_2,
                    "", ""))
            .build();
        SearchResultDefinition caseType2SearchResult = searchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID_2, CASE_FIELD_4, "", CASE_FIELD_4,
                    "", ""))
            .build();
        when(searchResultDefinitionService.getSearchResultDefinition(any(), any(), any()))
            .thenReturn(caseType1SearchResult, caseType2SearchResult);
        doAnswer(i -> i.getArgument(1)).when(dateTimeSearchResultProcessor).execute(any(), any());
        when(securityClassificationService.userHasEnoughSecurityClassificationForField(any(), any(), any()))
            .thenReturn(true);

        classUnderTest = new CaseSearchResultViewGenerator(caseTypeService, searchResultDefinitionService,
            dateTimeSearchResultProcessor, caseSearchesViewAccessControl, caseDataAccessControl);

        when(caseDataAccessControl.generateAccessMetadata(anyString()))
            .thenReturn(new CaseAccessMetadata());
    }

    private static Set<AccessProfile> createAccessProfiles(Set<String> userRoles) {
        return userRoles.stream()
            .map(userRole -> AccessProfile.builder().readOnly(false)
                .accessProfile(userRole)
                .build())
            .collect(Collectors.toSet());
    }


    @Test
    void shouldBuildCaseSearchResultHeaders() {

        final CaseSearchResultView caseSearchResultView = classUnderTest.execute(CASE_TYPE_ID_1, caseSearchResult,
            WORKBASKET, Collections.emptyList());

        assertAll(
            () -> assertThat(caseSearchResultView.getHeaders().size(), is(1)),
            () -> assertMetadata(caseSearchResultView.getHeaders().get(0).getMetadata(), CASE_TYPE_ID_1, JURISDICTION),
            () -> assertCasesList(caseSearchResultView.getHeaders().get(0).getCases(), 2,
                "999", "1000"),
            () -> assertThat(caseSearchResultView.getHeaders().get(0).getFields().size(), is(2)),
            () -> assertHeaderField(caseSearchResultView.getHeaders().get(0).getFields().get(0), CASE_FIELD_1,
                CASE_FIELD_1, TEXT_TYPE)
        );
    }

    @Test
    void shouldBuildCaseSearchResultCases() {
        final CaseSearchResultView caseSearchResultView = classUnderTest.execute(CASE_TYPE_ID_1, caseSearchResult,
            "ORGCASES", Collections.emptyList());

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
            () -> assertThat(caseSearchResultView.getCases().get(0).getFields().get("[JURISDICTION]"),
                is(JURISDICTION)),
            () -> assertThat(caseSearchResultView.getCases().get(0).getFields().get("[STATE]"), is("state1")),
            () -> assertThat(caseSearchResultView.getCases().get(0).getFields().get("[LAST_STATE_MODIFIED_DATE]"),
                is(LAST_STATE_MODIFIED_DATE)),
            () -> assertThat(caseSearchResultView.getCases().get(0).getFields().get("[CREATED_DATE]"),
                is(CREATED_DATE)),
            () -> assertThat(caseSearchResultView.getCases().get(0).getFields().get("[CASE_REFERENCE]"), is("999")),
            () -> assertThat(caseSearchResultView.getCases().get(0).getFields().get("[SECURITY_CLASSIFICATION]"),
                    is(SECURITY_CLASSIFICATION)),
            () -> assertThat(caseSearchResultView.getCases().get(0).getFields().get("[CASE_TYPE]"), is(CASE_TYPE_ID_1)),
            () -> assertThat(caseSearchResultView.getCases().get(0).getFields().get("[LAST_MODIFIED_DATE]"),
                    is(LAST_MODIFIED_DATE)),
            () -> assertThat(caseSearchResultView.getCases().get(0).getSupplementaryData().size(), is(2)),
            () -> assertThat(caseSearchResultView.getCases().get(0).getSupplementaryData()
                            .get(SUPPLEMENTARY_DATA_FIELD_1).asText(),
                is(SUPPLEMENTARY_DATA_FIELD_1)),
            () -> assertThat(caseSearchResultView.getCases().get(0).getSupplementaryData()
                            .get(SUPPLEMENTARY_DATA_FIELD_2).asText(),
                is(SUPPLEMENTARY_DATA_FIELD_2)),
            () -> assertThat(caseSearchResultView.getCases().get(1).getSupplementaryData(), is(nullValue())),
            () -> assertThat(caseSearchResultView.getCases().get(2).getSupplementaryData(), is(nullValue()))
        );
    }

    @Test
    void shouldBuildCaseSearchResultTotal() {
        final CaseSearchResultView searchResultView =
            classUnderTest.execute(CASE_TYPE_ID_1, caseSearchResult, WORKBASKET, Collections.emptyList());

        assertAll(
            () -> assertThat(searchResultView.getTotal(), is(3L))
        );
    }

    @Test
    void shouldBuildHeaderFieldsWithComplexFields() {
        SearchResultDefinition searchResult = searchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_1, "", CASE_FIELD_1,
                    "", ""),
                buildSearchResultField(CASE_TYPE_ID_1, FAMILY_DETAILS, FATHER_NAME, FATHER_NAME,
                    "", ""),
                buildSearchResultField(CASE_TYPE_ID_1, FAMILY_DETAILS, MOTHER_NAME, MOTHER_NAME,
                    "", ""))
            .build();
        when(searchResultDefinitionService.getSearchResultDefinition(any(), any(), any())).thenReturn(searchResult);


        final CaseSearchResultView caseSearchResultView = classUnderTest.execute(CASE_TYPE_ID_1, caseSearchResult,
            WORKBASKET, Collections.emptyList());

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
        CaseTypeDefinition caseTypeDefinition = CaseTypeDefinition.builder()
            .id(CASE_TYPE_ID_1)
            .jurisdictionDefinition(jurisdiction)
            .securityClassification(SecurityClassification.PUBLIC)
            .caseFieldDefinitions(List.of(
                CaseFieldDefinition.builder().id(CASE_FIELD_1)
                    .caseTypeId(CASE_TYPE_ID_1)
                    .fieldTypeDefinition(textFieldType()).accessControlLists(List.of(anAcl()
                        .withRole(ROLE_IN_USER_ROLE_1)
                        .withRead(true)
                        .build())).build(),
                CaseFieldDefinition.builder().id(CASE_FIELD_2)
                    .caseTypeId(CASE_TYPE_ID_1)
                    .fieldTypeDefinition(textFieldType()).accessControlLists(List.of(anAcl()
                        .withRole(ROLE_IN_USER_ROLE_1)
                        .withRead(true)
                        .build())).build(),
                CaseFieldDefinition.builder().id(CASE_FIELD_4)
                    .caseTypeId(CASE_TYPE_ID_1)
                    .fieldTypeDefinition(textFieldType()).accessControlLists(List.of(anAcl()
                        .withRole(ROLE_IN_USER_ROLE_1)
                        .withRead(true)
                        .build())).build(),
                CaseFieldDefinition.builder().id(CASE_FIELD_5)
                    .caseTypeId(CASE_TYPE_ID_1)
                    .fieldTypeDefinition(textFieldType()).accessControlLists(List.of(anAcl()
                        .withRole(ROLE_IN_USER_ROLE_1)
                        .withRead(true)
                        .build())).build()
            ))
            .build();

        SearchResultField searchResultFieldWithValidRole = buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_4,
            "", CASE_FIELD_4, "", "");
        searchResultFieldWithValidRole.setRole(ROLE_IN_USER_ROLE_1);
        SearchResultField searchResultFieldWithInvalidRole = buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_5,
            "", CASE_FIELD_5, "", "");
        searchResultFieldWithInvalidRole.setRole(ROLE_NOT_IN_USER_ROLE);
        SearchResultDefinition searchResult = searchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_1, "", CASE_FIELD_1,
                    "", ""),
                buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_2, "", CASE_FIELD_2,
                    "", ""),
                searchResultFieldWithValidRole,
                searchResultFieldWithInvalidRole)
            .build();
        when(caseTypeService.getCaseType(eq(CASE_TYPE_ID_1))).thenReturn(caseTypeDefinition);
        when(searchResultDefinitionService.getSearchResultDefinition(any(), any(), any())).thenReturn(searchResult);


        when(caseDataAccessControl.anyAccessProfileEqualsTo(anyString(), anyString())).thenReturn(true);
        when(caseDataAccessControl.anyAccessProfileEqualsTo(CASE_TYPE_ID_1,
            searchResultFieldWithInvalidRole.getRole())).thenReturn(false);

        final CaseSearchResultView caseSearchResultView = classUnderTest.execute(CASE_TYPE_ID_1, caseSearchResult,
            WORKBASKET, Collections.emptyList());
        when(caseDataAccessControl.anyAccessProfileEqualsTo(anyString(), anyString())).thenReturn(true);

        assertAll(
            () -> assertThat(caseSearchResultView.getHeaders().get(0).getFields().size(), is(3)),
            () -> assertThat(caseSearchResultView.getHeaders().get(0).getFields().get(0).getCaseFieldId(),
                is(CASE_FIELD_1)),
            () -> assertThat(caseSearchResultView.getHeaders().get(0).getFields().get(1).getCaseFieldId(),
                is(CASE_FIELD_2)),
            () -> assertThat(caseSearchResultView.getHeaders().get(0).getFields().get(2).getCaseFieldId(),
                is(CASE_FIELD_4))
        );
    }

    @Test
    void shouldBuildHeaderFieldsWithNoDuplicateColumnsForMultiplePermittedRoles() {
        SearchResultField searchResultFieldWithValidRole = buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_4,
            "", CASE_FIELD_4, "", "");
        searchResultFieldWithValidRole.setRole(ROLE_IN_USER_ROLE_1);
        SearchResultField searchResultFieldWithValidRole2 = buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_4,
            "", CASE_FIELD_4, "", "");
        searchResultFieldWithValidRole2.setRole(ROLE_IN_USER_ROLE_2);
        SearchResultField searchResultFieldWithInvalidRole = buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_5,
            "", CASE_FIELD_5, "", "");
        searchResultFieldWithInvalidRole.setRole(ROLE_NOT_IN_USER_ROLE);
        SearchResultDefinition searchResult = searchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_1, "", CASE_FIELD_1,
                    "", ""),
                buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_1, "", CASE_FIELD_1,
                    "", ""),
                buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_1, "", CASE_FIELD_1,
                    "", ""),
                buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_2, "", CASE_FIELD_2,
                    "", ""),
                buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_2, "", CASE_FIELD_2,
                    "", ""),
                searchResultFieldWithValidRole,
                searchResultFieldWithValidRole2,
                searchResultFieldWithInvalidRole)
            .build();
        CaseTypeDefinition caseTypeDefinition = CaseTypeDefinition.builder()
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
                CaseFieldDefinition.builder().id(CASE_FIELD_2)
                    .caseTypeId(CASE_TYPE_ID_1)
                    .fieldTypeDefinition(textFieldType()).accessControlLists(List.of(anAcl()
                        .withRole(ROLE_IN_USER_ROLE_1)
                        .withRead(true)
                        .build())).build(),
                CaseFieldDefinition.builder().id(CASE_FIELD_4)
                    .caseTypeId(CASE_TYPE_ID_1)
                    .fieldTypeDefinition(textFieldType()).accessControlLists(List.of(anAcl()
                        .withRole(ROLE_IN_USER_ROLE_1)
                        .withRead(true)
                        .build())).build(),
                CaseFieldDefinition.builder().id(CASE_FIELD_5)
                    .caseTypeId(CASE_TYPE_ID_1)
                    .fieldTypeDefinition(textFieldType()).accessControlLists(List.of(anAcl()
                        .withRole(ROLE_IN_USER_ROLE_1)
                        .withRead(true)
                        .build())).build()
                ))
            .build();

        when(caseTypeService.getCaseType(eq(CASE_TYPE_ID_1))).thenReturn(caseTypeDefinition);
        when(searchResultDefinitionService.getSearchResultDefinition(any(), any(), any())).thenReturn(searchResult);


        when(caseDataAccessControl.anyAccessProfileEqualsTo(anyString(), anyString())).thenReturn(true);
        when(caseDataAccessControl.anyAccessProfileEqualsTo(CASE_TYPE_ID_1,
            searchResultFieldWithInvalidRole.getRole())).thenReturn(false);

        final CaseSearchResultView caseSearchResultView =
            classUnderTest.execute(CASE_TYPE_ID_1, caseSearchResult, WORKBASKET, Collections.emptyList());

        assertAll(
            () -> assertThat(caseSearchResultView.getHeaders().get(0).getFields().size(), is(3)),
            () -> assertThat(caseSearchResultView.getHeaders().get(0).getFields().get(0).getCaseFieldId(),
                is(CASE_FIELD_1)),
            () -> assertThat(caseSearchResultView.getHeaders().get(0).getFields().get(1).getCaseFieldId(),
                is(CASE_FIELD_2)),
            () -> assertThat(caseSearchResultView.getHeaders().get(0).getFields().get(2).getCaseFieldId(),
                is(CASE_FIELD_4))
        );
    }

    @Test
    void shouldBuildResultsWithComplexNestedElements() {
        SearchResultField searchResultFieldWithValidRole =
            buildSearchResultField(CASE_TYPE_ID_1, FAMILY_DETAILS, FAMILY_DETAILS_PATH, FAMILY_DETAILS,
                "", "");
        searchResultFieldWithValidRole.setRole(ROLE_IN_USER_ROLE_1);
        SearchResultField searchResultFieldWithValidRoleNested =
            buildSearchResultField(CASE_TYPE_ID_1, FAMILY_DETAILS, FAMILY_DETAILS_PATH_NESTED, FAMILY_DETAILS,
                "", "");
        searchResultFieldWithValidRoleNested.setRole(ROLE_IN_USER_ROLE_1);

        SearchResultDefinition searchResult = searchResult()
            .withSearchResultFields(
                searchResultFieldWithValidRole,
                searchResultFieldWithValidRoleNested)
            .build();

        when(searchResultDefinitionService.getSearchResultDefinition(any(), any(), any())).thenReturn(searchResult);

        doReturn(true).when(caseDataAccessControl)
            .anyAccessProfileEqualsTo(CASE_TYPE_ID_1, searchResultFieldWithValidRole.getRole());


        CaseSearchResultView caseSearchResultView = classUnderTest.execute(CASE_TYPE_ID_1, caseSearchResult, WORKBASKET,
            Collections.emptyList());

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
    void shouldBuildResultsWithCaseAccessMetadata() {
        CaseAccessMetadata caseAccessMetadata = new CaseAccessMetadata();
        caseAccessMetadata.setAccessGrants(List.of(GrantType.SPECIFIC, GrantType.BASIC));
        caseAccessMetadata.setAccessProcess(AccessProcess.CHALLENGED);

        when(caseDataAccessControl.generateAccessMetadata(anyString()))
            .thenReturn(caseAccessMetadata);

        CaseSearchResultView caseSearchResultView = classUnderTest.execute(CASE_TYPE_ID_1, caseSearchResult, WORKBASKET,
            Collections.emptyList());

        assertAll(
            () -> assertThat(((TextNode) caseSearchResultView.getCases().get(0).getFields()
                .get(CaseAccessMetadata.ACCESS_PROCESS)).asText(), is(AccessProcess.CHALLENGED.name())),
            () -> assertThat(((TextNode) caseSearchResultView.getCases().get(0).getFields()
                .get(CaseAccessMetadata.ACCESS_GRANTED)).asText(),
                is(GrantType.BASIC.name() + "," + GrantType.SPECIFIC.name()))
        );
    }

    @Test
    void shouldInvokeSearchProcessorDCPIsProvided() {
        SearchResultDefinition searchResult = searchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_1, "", CASE_FIELD_1,
                    "#DCP", ""))
            .build();
        when(searchResultDefinitionService.getSearchResultDefinition(any(), any(), any())).thenReturn(searchResult);

        classUnderTest.execute(CASE_TYPE_ID_1, caseSearchResult, WORKBASKET, Collections.emptyList());

        verify(dateTimeSearchResultProcessor).execute(any(), any());
    }

    @Test
    void shouldNotInvokeSearchProcessorWhenNoDCPProvided() {
        SearchResultDefinition searchResult = searchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID_1, CASE_FIELD_1, "", CASE_FIELD_1,
                    null, ""))
            .build();
        when(searchResultDefinitionService.getSearchResultDefinition(any(), any(), any())).thenReturn(searchResult);

        classUnderTest.execute(CASE_TYPE_ID_1, caseSearchResult, WORKBASKET, Collections.emptyList());

        verifyNoMoreInteractions(dateTimeSearchResultProcessor);
    }

    @Test
    void shouldThrowBadRequestExceptionWhenNoNestedElementFoundForPath() {
        SearchResultDefinition searchResult = searchResult()
            .withSearchResultFields(buildSearchResultField(CASE_TYPE_ID_1,
                FAMILY_DETAILS, "InvalidElement",
                FAMILY_DETAILS, "", ""))
            .build();
        when(searchResultDefinitionService.getSearchResultDefinition(any(), any(), any())).thenReturn(searchResult);

        final BadRequestException exception = assertThrows(BadRequestException.class,
            () -> classUnderTest.execute(CASE_TYPE_ID_1, caseSearchResult, WORKBASKET, Collections.emptyList()));

        assertAll(
            () -> assertThat(exception.getMessage(), is("CaseField FamilyDetails has no nested elements with code "
                + "InvalidElement."))
        );
    }

    @Test
    void shouldNotNotReturnHeaderFieldsWhenNoNestedElementFoundForPath() {
        SearchResultDefinition searchResult = searchResult()
            .withSearchResultFields(buildSearchResultField(CASE_TYPE_ID_1,
                FAMILY_DETAILS, "InvalidElementPath",
                FAMILY_DETAILS, "", ""))
            .build();
        CaseTypeDefinition caseTypeWithoutCaseFieldDefinition = CaseTypeDefinition.builder()
            .id(CASE_TYPE_ID_1)
            .jurisdictionDefinition(jurisdiction)
            .securityClassification(SecurityClassification.PUBLIC)
            .caseFieldDefinitions(List.of(
                CaseFieldDefinition.builder().id(CASE_FIELD_1)
                    .fieldTypeDefinition(textFieldType()).accessControlLists(List.of(anAcl()
                    .withRole(ROLE_IN_USER_ROLE_1)
                    .withRead(true)
                    .build())).build()
            ))
            .build();

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
            () -> assertThat(exception.getMessage(), is("The provided use case 'INVALID' is unsupported for "
                   + "case type 'CASE_TYPE_1'."))
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
            () -> IntStream.range(0, actualValues.size()).forEach(idx -> assertThat(actualValues.get(idx),
                is(expectedValues[idx])))
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
        return FieldTypeDefinition.builder().id(TEXT_TYPE).type(TEXT_TYPE).build();
    }
}
