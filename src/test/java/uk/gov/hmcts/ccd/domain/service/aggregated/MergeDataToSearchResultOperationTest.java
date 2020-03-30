package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Sets;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResult;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResultField;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultView;
import uk.gov.hmcts.ccd.domain.service.processor.SearchResultProcessor;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.STATE;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COMPLEX;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.LABEL;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.SearchResultBuilder.aSearchResult;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.buildData;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.buildSearchResultField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class MergeDataToSearchResultOperationTest {
    private static final String CASE_TYPE_ID = "CASE_TYPE";
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
    private static final String FAMILY_DETAILS_VALUE = "{\"FatherName\":\"" + FATHER_NAME_VALUE + "\"," +
        "\"MotherName\":\"" + MOTHER_NAME_VALUE + "\"," +
        "\"FamilyAddress\":{" +
        "\"County\":\"\"," +
        "\"Country\":\"United Kingdom\"," +
        "\"PostCode\":\"" + POSTCODE_VALUE + "\"," +
        "\"PostTown\":\"London\"," +
        "\"AddressLine1\":\"40 Edric House\"," +
        "\"AddressLine2\":\"\",\"AddressLine3\":\"\"}" +
        "}";
    private static final String FAMILY_DETAILS_PATH = "FatherName";
    private static final String FAMILY_DETAILS_PATH_NESTED = "FamilyAddress.PostCode";

    private static final String TEXT_TYPE = "Text";
    private static final String LABEL_ID = "LabelId";
    private static final String LABEL_TEXT = "LabelText";
    private static final String NO_ERROR = null;
    private static final String TIMEOUT_ERROR = "Error while retrieving drafts.";

    private static final String FAMILY = "FamilyDetails";
    private static final String FATHER_NAME = "FatherName";
    private static final String MOTHER_NAME = "MotherName";

    private static final String FAMILY_ADDRESS = "FamilyAddress";
    private static final String ADDRESS_LINE_1 = "AddressLine1";
    private static final String POSTCODE = "PostCode";

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private SearchResultProcessor searchResultProcessor;
    
    private MergeDataToSearchResultOperation classUnderTest;

    private List<CaseDetails> caseDetailsList;
    private CaseType caseType;
    private CaseType caseTypeWithLabels;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);

        Map<String, JsonNode> dataMap = buildData(CASE_FIELD_1, CASE_FIELD_2, CASE_FIELD_3, CASE_FIELD_4);
        ObjectNode familyDetails = (ObjectNode) new ObjectMapper().readTree(FAMILY_DETAILS_VALUE);
        dataMap.put(FAMILY_DETAILS, familyDetails);

        CaseDetails caseDetails1 = new CaseDetails();
        caseDetails1.setReference(999L);
        caseDetails1.setData(dataMap);
        caseDetails1.setState("state1");

        CaseDetails caseDetails2 = new CaseDetails();
        caseDetails2.setReference(1000L);
        caseDetails2.setData(dataMap);
        caseDetails2.setState("state2");

        caseDetailsList = Arrays.asList(caseDetails1, caseDetails2);

        final CaseField fatherName = newCaseField().withId(FATHER_NAME).withFieldType(textFieldType()).build();
        final CaseField motherName = newCaseField().withId(MOTHER_NAME).withFieldType(textFieldType()).build();

        final CaseField addressLine1 = newCaseField().withId(ADDRESS_LINE_1).withFieldType(textFieldType()).build();
        final CaseField postCode = newCaseField().withId(POSTCODE).withFieldType(textFieldType()).build();
        final FieldType addressFieldType = aFieldType().withId(FAMILY_ADDRESS).withType(COMPLEX)
            .withComplexField(addressLine1).withComplexField(postCode).build();
        final CaseField familyAddress = newCaseField().withId(FAMILY_ADDRESS).withFieldType(addressFieldType).build();

        final FieldType familyDetailsFieldType =
            aFieldType().withId(FAMILY).withType(COMPLEX)
                .withComplexField(fatherName)
                .withComplexField(motherName)
                .withComplexField(familyAddress)
                .build();

        caseType = newCaseType()
            .withCaseTypeId(CASE_TYPE_ID)
            .withField(newCaseField().withId(CASE_FIELD_1).withFieldType(textFieldType()).build())
            .withField(newCaseField().withId(CASE_FIELD_2).withFieldType(textFieldType()).build())
            .withField(newCaseField().withId(CASE_FIELD_3).withFieldType(textFieldType()).build())
            .withField(newCaseField().withId(CASE_FIELD_4).withFieldType(textFieldType()).build())
            .withField(newCaseField().withId(CASE_FIELD_5).withFieldType(textFieldType()).build())
            .withField(newCaseField().withId(FAMILY_DETAILS).withFieldType(familyDetailsFieldType).build())
            .build();

        final CaseField labelField = buildLabelCaseField(LABEL_ID, LABEL_TEXT);
        caseTypeWithLabels = newCaseType()
            .withCaseTypeId(CASE_TYPE_ID)
            .withField(newCaseField().withId(CASE_FIELD_1).withFieldType(textFieldType()).build())
            .withField(newCaseField().withId(CASE_FIELD_2).withFieldType(textFieldType()).build())
            .withField(newCaseField().withId(CASE_FIELD_3).withFieldType(textFieldType()).build())
            .withField(labelField)
            .build();
        doReturn(Collections.emptySet()).when(userRepository).getUserRoles();
        doAnswer(i -> new SearchResultView(i.getArgument(0), i.getArgument(1), i.getArgument(2)))
            .when(searchResultProcessor).execute(Mockito.any(), Mockito.any(), Mockito.any());

        classUnderTest = new MergeDataToSearchResultOperation(userRepository, searchResultProcessor);
    }

    @Test
    @DisplayName("should get Workbasket Results with defined columns")
    void getWorkbasketView() {
        SearchResult searchResult = aSearchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_1, "", CASE_FIELD_1, ""),
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_2, "", CASE_FIELD_2, ""))
            .build();

        final SearchResultView searchResultView = classUnderTest.execute(caseType, searchResult, caseDetailsList, NO_ERROR);
        assertAll(
            () -> assertThat(searchResultView.getSearchResultViewItems().size(), is(2)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().size(), is(2)),
            () -> assertThat(searchResultView.getSearchResultViewItems().get(0).getCaseFields(),
                is(not(sameInstance(searchResultView.getSearchResultViewItems().get(0).getCaseFieldsFormatted())))),
            () -> assertThat(searchResultView.getSearchResultViewItems().get(1).getCaseFields(),
                is(not(sameInstance(searchResultView.getSearchResultViewItems().get(1).getCaseFieldsFormatted())))),
            () -> assertThat(searchResultView.getSearchResultViewItems().get(0).getCaseFields().get(STATE.getReference()), is("state1")),
            () -> assertThat(searchResultView.getSearchResultViewItems().get(1).getCaseFields().get(STATE.getReference()), is("state2")),
            () -> assertThat(searchResultView.getSearchResultViewItems().get(0).getCaseFieldsFormatted().get(STATE.getReference()), is("state1")),
            () -> assertThat(searchResultView.getSearchResultViewItems().get(1).getCaseFieldsFormatted().get(STATE.getReference()), is("state2")),
            () -> assertThat(searchResultView.getSearchResultViewItems().get(1).getCaseFields().get(STATE.getReference()),
                is(searchResultView.getSearchResultViewItems().get(1).getCaseFieldsFormatted().get(STATE.getReference()))),
            () -> assertThat(searchResultView.getResultError(), is(NO_ERROR))
                 );
    }

    @Test
    @DisplayName("should get Workbasket Results with defined complex field columns")
    void getWorkbasketViewWithComplexFields() {
        SearchResult searchResult = aSearchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_1, "", CASE_FIELD_1, ""),
                buildSearchResultField(CASE_TYPE_ID, FAMILY_DETAILS, FATHER_NAME, FATHER_NAME, ""),
                buildSearchResultField(CASE_TYPE_ID, FAMILY_DETAILS, MOTHER_NAME, MOTHER_NAME, ""))
            .build();

        final SearchResultView searchResultView = classUnderTest.execute(caseType, searchResult, caseDetailsList, NO_ERROR);
        assertAll(
            () -> assertThat(searchResultView.getSearchResultViewColumns().size(), is(3)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().get(0).getLabel(), is(CASE_FIELD_1)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().get(1).getLabel(), is(FATHER_NAME)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().get(2).getLabel(), is(MOTHER_NAME)),
            () -> assertThat(searchResultView.getResultError(), is(NO_ERROR))
        );
    }

    @Test
    @DisplayName("should get Workbasket Results with defined columns and allowed roles")
    void getWorkbasketViewWithValidRoles() {
        SearchResultField searchResultFieldWithValidRole = buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_4, "",   CASE_FIELD_4, "");
        searchResultFieldWithValidRole.setRole(ROLE_IN_USER_ROLE_1);
        SearchResultField searchResultFieldWithInvalidRole = buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_5, "", CASE_FIELD_5, "");
        searchResultFieldWithInvalidRole.setRole(ROLE_NOT_IN_USER_ROLE);
        SearchResult searchResult = aSearchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_1, "", CASE_FIELD_1, ""),
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_2, "", CASE_FIELD_2, ""),
                searchResultFieldWithValidRole,
                searchResultFieldWithInvalidRole)
            .build();

        doReturn(Sets.newHashSet(ROLE_IN_USER_ROLE_1)).when(userRepository).getUserRoles();

        final SearchResultView searchResultView = classUnderTest.execute(caseType, searchResult, caseDetailsList, NO_ERROR);
        assertAll(
            () -> assertThat(searchResultView.getSearchResultViewItems().size(), is(2)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().size(), is(3)),
            () -> assertThat(searchResultView.getSearchResultViewItems().get(0).getCaseFields().get(STATE.getReference()), is("state1")),
            () -> assertThat(searchResultView.getSearchResultViewItems().get(1).getCaseFields().get(STATE.getReference()), is("state2")),
            () -> assertThat(searchResultView.getSearchResultViewColumns().get(0).getCaseFieldId(), is(CASE_FIELD_1)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().get(1).getCaseFieldId(), is(CASE_FIELD_2)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().get(2).getCaseFieldId(), is(CASE_FIELD_4)),
            () -> assertThat(searchResultView.getResultError(), is(NO_ERROR))
                 );
    }

    @Test
    @DisplayName("should get Workbasket Results with defined columns for allowed roles and no duplicate columns")
    void getWorkbasketViewWithValidRolesAndNoDuplicateColumns() {
        SearchResultField searchResultFieldWithValidRole = buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_4, "", CASE_FIELD_4, "");
        searchResultFieldWithValidRole.setRole(ROLE_IN_USER_ROLE_1);
        SearchResultField searchResultFieldWithValidRole2 = buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_4, "", CASE_FIELD_4, "");
        searchResultFieldWithValidRole2.setRole(ROLE_IN_USER_ROLE_2);
        SearchResultField searchResultFieldWithInvalidRole = buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_5, "", CASE_FIELD_5, "");
        searchResultFieldWithInvalidRole.setRole(ROLE_NOT_IN_USER_ROLE);
        SearchResult searchResult = aSearchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_1, "", CASE_FIELD_1, ""),
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_1, "", CASE_FIELD_1, ""),
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_1, "", CASE_FIELD_1, ""),
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_2, "", CASE_FIELD_2, ""),
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_2, "", CASE_FIELD_2, ""),
                searchResultFieldWithValidRole,
                searchResultFieldWithValidRole2,
                searchResultFieldWithInvalidRole)
            .build();

        doReturn(Sets.newHashSet(ROLE_IN_USER_ROLE_1, ROLE_IN_USER_ROLE_2)).when(userRepository).getUserRoles();

        final SearchResultView searchResultView = classUnderTest.execute(caseType, searchResult, caseDetailsList, NO_ERROR);
        assertAll(
            () -> assertThat(searchResultView.getSearchResultViewItems().size(), is(2)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().size(), is(3)),
            () -> assertThat(searchResultView.getSearchResultViewItems().get(0).getCaseFields().get(STATE.getReference()), is("state1")),
            () -> assertThat(searchResultView.getSearchResultViewItems().get(1).getCaseFields().get(STATE.getReference()), is("state2")),
            () -> assertThat(searchResultView.getSearchResultViewColumns().get(0).getCaseFieldId(), is(CASE_FIELD_1)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().get(1).getCaseFieldId(), is(CASE_FIELD_2)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().get(2).getCaseFieldId(), is(CASE_FIELD_4)),
            () -> assertThat(searchResultView.getResultError(), is(NO_ERROR))
                 );
    }

    @Test
    @DisplayName("should process result fields with valid and invalid roles correctly")
    void getWorkbasketViewWithValidRolesAndInvalidRoles() {
        SearchResultField searchResultFieldWithInvalidRole1 = buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_4, "", CASE_FIELD_4, "");
        searchResultFieldWithInvalidRole1.setRole(ROLE_NOT_IN_USER_ROLE);
        SearchResultField searchResultFieldWithValidRole = buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_4, "", CASE_FIELD_4, "");
        searchResultFieldWithValidRole.setRole(ROLE_IN_USER_ROLE_2);
        SearchResultField searchResultFieldWithInvalidRole2 = buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_5, "", CASE_FIELD_5, "");
        searchResultFieldWithInvalidRole2.setRole(ROLE_NOT_IN_USER_ROLE);
        SearchResult searchResult = aSearchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_1, "", CASE_FIELD_1, ""),
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_1, "", CASE_FIELD_1, ""),
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_2, "", CASE_FIELD_2, ""),
                searchResultFieldWithInvalidRole1,
                searchResultFieldWithInvalidRole2,
                searchResultFieldWithValidRole)
            .build();

        doReturn(Sets.newHashSet(ROLE_IN_USER_ROLE_1, ROLE_IN_USER_ROLE_2)).when(userRepository).getUserRoles();


        final SearchResultView searchResultView = classUnderTest.execute(caseType, searchResult, caseDetailsList, NO_ERROR);
        assertAll(
            () -> assertThat(searchResultView.getSearchResultViewItems().size(), is(2)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().size(), is(3)),
            () -> assertThat(searchResultView.getSearchResultViewItems().get(0).getCaseFields().get(STATE.getReference()), is("state1")),
            () -> assertThat(searchResultView.getSearchResultViewItems().get(1).getCaseFields().get(STATE.getReference()), is("state2")),
            () -> assertThat(searchResultView.getSearchResultViewColumns().get(0).getCaseFieldId(), is(CASE_FIELD_1)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().get(1).getCaseFieldId(), is(CASE_FIELD_2)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().get(2).getCaseFieldId(), is(CASE_FIELD_4)),
            () -> assertThat(searchResultView.getResultError(), is(NO_ERROR))
                 );
    }

    @Test
    @DisplayName("should get Workbasket Results with defined columns and Labels")
    void getWorkbasketViewAndLabels() {

        SearchResult searchResult = aSearchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_1, "", CASE_FIELD_1, ""),
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_2, "", CASE_FIELD_2, ""))
            .build();

        final SearchResultView searchResultView = classUnderTest.execute(caseTypeWithLabels,
            searchResult, caseDetailsList,
            NO_ERROR);
        assertAll(
            () -> assertThat(searchResultView.getSearchResultViewItems().size(), is(2)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().size(), is(2)),
            () -> assertThat(((TextNode)searchResultView.getSearchResultViewItems().get(0)
                .getCaseFields()
                .get(LABEL_ID)).asText(), is(LABEL_TEXT)),
            () -> assertThat(((TextNode)searchResultView.getSearchResultViewItems().get(1)
                .getCaseFields()
                .get(LABEL_ID)).asText(), is(LABEL_TEXT)));
    }

    @Test
    @DisplayName("should get Search Results with defined columns")
    void getSearchView() {
        SearchResult searchResult = aSearchResult()
            .withSearchResultFields(buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_2, "", CASE_FIELD_2, ""))
            .build();

        final SearchResultView searchResultView = classUnderTest.execute(caseType, searchResult, caseDetailsList, TIMEOUT_ERROR);
        assertAll(
            () -> assertThat(searchResultView.getSearchResultViewItems().size(), is(2)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().size(), is(1)),
            () -> assertThat(searchResultView.getResultError(), is(TIMEOUT_ERROR))
                 );
    }

    @Test
    @DisplayName("should get Search Results with defined complex field columns")
    void getSearchViewWithComplexFields() {
        SearchResult searchResult = aSearchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_2, "", CASE_FIELD_2, ""),
                buildSearchResultField(CASE_TYPE_ID, FAMILY_DETAILS, FATHER_NAME, FATHER_NAME, ""),
                buildSearchResultField(CASE_TYPE_ID, FAMILY_DETAILS, MOTHER_NAME, MOTHER_NAME, ""))
            .build();

        final SearchResultView searchResultView = classUnderTest.execute(caseType, searchResult, caseDetailsList, NO_ERROR);
        assertAll(
            () -> assertThat(searchResultView.getSearchResultViewColumns().size(), is(3)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().get(0).getLabel(), is(CASE_FIELD_2)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().get(1).getLabel(), is(FATHER_NAME)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().get(2).getLabel(), is(MOTHER_NAME)),
            () -> assertThat(searchResultView.getResultError(), is(NO_ERROR))
        );
    }

    @Test
    @DisplayName("should get Search Results with defined columns and labels")
    void getSearchViewAndLabels() {

        SearchResult searchResult = aSearchResult()
            .withSearchResultFields(buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_2, "", CASE_FIELD_2, ""))
            .build();

        final SearchResultView searchResultView = classUnderTest.execute(caseTypeWithLabels,
            searchResult, caseDetailsList,
            NO_ERROR);
        assertAll(
            () -> assertThat(searchResultView.getSearchResultViewItems().size(), is(2)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().size(), is(1)),
            () -> assertThat(((TextNode)searchResultView.getSearchResultViewItems().get(0).getCaseFields()
                .get(LABEL_ID)).asText(), is(LABEL_TEXT)),
            () -> assertThat(((TextNode)searchResultView.getSearchResultViewItems().get(1).getCaseFields()
                .get(LABEL_ID)).asText(), is(LABEL_TEXT)))
        ;
    }

    @Test
    @DisplayName("should get Search Results when SearchResult elements have caseFieldPath specified")
    void getSearchViewWhenCaseFieldPathDefined() {

        SearchResult searchResult = aSearchResult()
            .withSearchResultFields(buildSearchResultField(CASE_TYPE_ID,
                FAMILY_DETAILS, FAMILY_DETAILS_PATH,
                FAMILY_DETAILS, ""))
            .build();

        final SearchResultView searchResultView = classUnderTest.execute(caseType,
            searchResult, caseDetailsList,
            NO_ERROR);

        assertAll(
            () -> assertThat(searchResultView.getSearchResultViewItems().size(), is(2)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().size(), is(1)),
            () -> assertThat(((TextNode)searchResultView.getSearchResultViewItems().get(0).getCaseFields()
                .get("FamilyDetails.FatherName")).asText(), is(FATHER_NAME_VALUE)),
            () -> assertThat(((TextNode)searchResultView.getSearchResultViewItems().get(1).getCaseFields()
                .get("FamilyDetails.FatherName")).asText(), is(FATHER_NAME_VALUE)))
        ;
    }

    @Test
    @DisplayName("should get Results when SearchResult elements have caseFieldPath with nesting level of two")
    void getSearchViewWhenCaseFieldPathDefinedWithNestingLevelOfTwo() {

        SearchResult searchResult = aSearchResult()
            .withSearchResultFields(buildSearchResultField(CASE_TYPE_ID,
                FAMILY_DETAILS, FAMILY_DETAILS_PATH_NESTED,
                FAMILY_DETAILS, ""))
            .build();

        final SearchResultView searchResultView = classUnderTest.execute(caseType,
            searchResult, caseDetailsList,
            NO_ERROR);

        assertAll(
            () -> assertThat(searchResultView.getSearchResultViewItems().size(), is(2)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().size(), is(1)),
            () -> assertThat(((TextNode)searchResultView.getSearchResultViewItems().get(0).getCaseFields()
                .get("FamilyDetails.FamilyAddress.PostCode")).asText(), is(POSTCODE_VALUE)),
            () -> assertThat(((TextNode)searchResultView.getSearchResultViewItems().get(1).getCaseFields()
                .get("FamilyDetails.FamilyAddress.PostCode")).asText(), is(POSTCODE_VALUE)))
        ;
    }

    @Test
    @DisplayName("should throw BadRequestException for Search Results when no nested element found for the path")
    void throwsBadRequestExceptionWhenNoNestedElementFoundForPath() {

        SearchResult searchResult = aSearchResult()
            .withSearchResultFields(buildSearchResultField(CASE_TYPE_ID,
                FAMILY_DETAILS, "InvalidElement",
                FAMILY_DETAILS, ""))
            .build();

        final BadRequestException exception = assertThrows(BadRequestException.class,
            () -> classUnderTest.execute(caseType, searchResult, caseDetailsList, NO_ERROR));

        Assert.assertThat(exception.getMessage(),
            Matchers.is("CaseField " + FAMILY_DETAILS + " has no nested elements with code InvalidElement."));
    }

    @Test
    @DisplayName("should throw BadRequestException for Search Results when no nested element found for the path")
    void throwsBadRequestExceptionWhenNoNestedElementFoundForPath1() {

        SearchResult searchResult = aSearchResult()
            .withSearchResultFields(buildSearchResultField(CASE_TYPE_ID,
                FAMILY_DETAILS, "InvalidElementPath",
                FAMILY_DETAILS, ""))
            .build();

        final BadRequestException exception = assertThrows(BadRequestException.class,
            () -> {
                CaseType caseTypeWithoutCaseField = newCaseType().withCaseTypeId(CASE_TYPE_ID)
                    .withField(newCaseField().withId(CASE_FIELD_1).withFieldType(textFieldType()).build()).build();
                classUnderTest.execute(caseTypeWithoutCaseField, searchResult, caseDetailsList, NO_ERROR);
            });

        Assert.assertThat(exception.getMessage(),
            Matchers.is("Nested element not found for path InvalidElementPath"));
    }

    private FieldType textFieldType() {
        return aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build();
    }

    private CaseField buildLabelCaseField(final String labelId, final String labelText) {
        final CaseField caseField = newCaseField()
            .withId(labelId)
            .withFieldType(aFieldType()
                .withType(LABEL)
                .withId(UUID.randomUUID().toString())
                .build())
            .withFieldLabelText(labelText)
            .build();
        return caseField;
    }
}
