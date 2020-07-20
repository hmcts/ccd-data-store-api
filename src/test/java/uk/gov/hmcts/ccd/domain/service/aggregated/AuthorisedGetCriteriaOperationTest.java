package uk.gov.hmcts.ccd.domain.service.aggregated;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.SEARCH;
import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.WORKBASKET;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.SearchInputBuilder.aSearchInput;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.WorkbasketInputBuilder.aWorkbasketInput;

import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.search.SearchInput;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AuthorisedGetCriteriaOperationTest {
    private static final String CASE_TYPE_ONE = "CaseTypeOne";
    private static final String ROLE1 = "Role1";
    private static final String ROLE2 = "Role2";
    private static final String CASE_FIELD_ID_1_1 = "CASE_FIELD_1_1";
    private static final String CASE_FIELD_ID_1_2 = "CASE_FIELD_1_2";
    private static final String CASE_FIELD_ID_1_3 = "CASE_FIELD_1_3";
    private static final String CASE_FIELD_ID_1_4 = "CASE_FIELD_1_4";
    private static final String CASE_FIELD_ID_1_5 = "CASE_FIELD_1_5";
    private static final CaseFieldDefinition CASE_FIELD_1_1 = newCaseField().withId(CASE_FIELD_ID_1_1).build();
    private static final CaseFieldDefinition CASE_FIELD_1_2 = newCaseField().withId(CASE_FIELD_ID_1_2).build();
    private static final CaseFieldDefinition CASE_FIELD_1_3 = newCaseField().withId(CASE_FIELD_ID_1_3).build();
    private static final CaseFieldDefinition CASE_FIELD_1_5 = newCaseField().withId(CASE_FIELD_ID_1_5).build();
    private static List<WorkbasketInput> testWorkbasketInputs;
    private static List<SearchInput> testSearchInputs;
    @Mock
    private GetCriteriaOperation getCriteriaOperation;
    @Mock
    private GetCaseTypeOperation getCaseTypeOperation;
    @Mock
    private UserRepository userRepository;
    private AuthorisedGetCriteriaOperation classUnderTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        testWorkbasketInputs = Arrays.asList(
            aWorkbasketInput().withFieldId(CASE_FIELD_ID_1_1).build(),
            aWorkbasketInput().withFieldId(CASE_FIELD_ID_1_2).build(),
            aWorkbasketInput().withFieldId(CASE_FIELD_ID_1_3).build(),
            aWorkbasketInput().withFieldId(CASE_FIELD_ID_1_4).build(),
            aWorkbasketInput().withFieldId(CASE_FIELD_ID_1_5).withUserRole(ROLE1).build(),
            aWorkbasketInput().withFieldId(CASE_FIELD_ID_1_5).withUserRole(ROLE2).build()
        );
        testSearchInputs = Arrays.asList(
            aSearchInput().withFieldId(CASE_FIELD_ID_1_1).build(),
            aSearchInput().withFieldId(CASE_FIELD_ID_1_2).build(),
            aSearchInput().withFieldId(CASE_FIELD_ID_1_3).build(),
            aSearchInput().withFieldId(CASE_FIELD_ID_1_4).build(),
            aSearchInput().withFieldId(CASE_FIELD_ID_1_5).withUserRole(ROLE1).build(),
            aSearchInput().withFieldId(CASE_FIELD_ID_1_5).withUserRole(ROLE2).build()
        );
        CaseTypeDefinition testCaseTypeDefinition = newCaseType()
            .withField(CASE_FIELD_1_1)
            .withField(CASE_FIELD_1_2)
            .withField(CASE_FIELD_1_3)
            .withField(CASE_FIELD_1_5)
            .build();
        testCaseTypeDefinition.setId(CASE_TYPE_ONE);
        Optional<CaseTypeDefinition> testCaseTypeOpt = Optional.of(testCaseTypeDefinition);

        doReturn(testCaseTypeOpt).when(getCaseTypeOperation).execute(CASE_TYPE_ONE, CAN_READ);

        classUnderTest = new AuthorisedGetCriteriaOperation(getCriteriaOperation, getCaseTypeOperation, userRepository);
    }

    @Test
    @DisplayName("should fail when no case type due to no READ access type")
    void shouldFailWhenWhenNoReadAccess() {
        doReturn(Optional.empty()).when(getCaseTypeOperation).execute(CASE_TYPE_ONE, CAN_READ);

        assertThrows(ResourceNotFoundException.class, () -> classUnderTest.execute(CASE_TYPE_ONE, CAN_READ, WORKBASKET));
    }

    @Test
    @DisplayName("should return only authorised search input with ACL READ access")
    void shouldReturnOnlyAuthorisedSearchInputs() {
        doReturn(testSearchInputs).when(getCriteriaOperation).execute(CASE_TYPE_ONE, CAN_READ, SEARCH);

        final List<SearchInput> searchInputs = (List<SearchInput>) classUnderTest.execute(CASE_TYPE_ONE, CAN_READ, SEARCH);

        assertAll(
            () -> assertThat(searchInputs.size(), is(3)),
            () -> assertThat(searchInputs.get(0), is(testSearchInputs.get(0))),
            () -> assertThat(searchInputs.get(1), is(testSearchInputs.get(1))),
            () -> assertThat(searchInputs.get(2), is(testSearchInputs.get(2)))
        );
    }

    @Test
    @DisplayName("should return search input when user has necessary role")
    void shouldReturnSearchInputWhenRoleExists() {
        doReturn(testSearchInputs).when(getCriteriaOperation).execute(CASE_TYPE_ONE, CAN_READ, SEARCH);
        doReturn(Sets.newHashSet(ROLE1)).when(userRepository).getUserRoles();

        final List<SearchInput> searchInputs = (List<SearchInput>) classUnderTest.execute(CASE_TYPE_ONE, CAN_READ, SEARCH);

        assertAll(
            () -> assertThat(searchInputs.size(), is(4)),
            () -> assertThat(searchInputs.get(0), is(testSearchInputs.get(0))),
            () -> assertThat(searchInputs.get(1), is(testSearchInputs.get(1))),
            () -> assertThat(searchInputs.get(2), is(testSearchInputs.get(2))),
            () -> assertThat(searchInputs.get(3), is(testSearchInputs.get(4)))
        );
    }

    @Test
    @DisplayName("should not return duplicate search input when user has more than necessary role")
    void shouldReturnDistinctSearchInputWhenRoleExists() {
        doReturn(testSearchInputs).when(getCriteriaOperation).execute(CASE_TYPE_ONE, CAN_READ, SEARCH);
        doReturn(Sets.newHashSet(ROLE1, ROLE2)).when(userRepository).getUserRoles();

        final List<SearchInput> searchInputs = (List<SearchInput>) classUnderTest.execute(CASE_TYPE_ONE, CAN_READ, SEARCH);

        assertAll(
            () -> assertThat(searchInputs.size(), is(4)),
            () -> assertThat(searchInputs.get(0), is(testSearchInputs.get(0))),
            () -> assertThat(searchInputs.get(1), is(testSearchInputs.get(1))),
            () -> assertThat(searchInputs.get(2), is(testSearchInputs.get(2))),
            () -> assertThat(searchInputs.get(3), is(testSearchInputs.get(4)))
        );
    }

    @Test
    @DisplayName("Should return empty search inputs when no case field is authorised")
    void shouldReturnEmptySearchInputWhenNoFieldIsAuthorised() {
        doReturn(new ArrayList<>()).when(getCriteriaOperation).execute(CASE_TYPE_ONE, CAN_READ, SEARCH);

        final List<SearchInput> searchInputs = (List<SearchInput>) classUnderTest.execute(CASE_TYPE_ONE, CAN_READ, SEARCH);

        assertAll(
            () -> assertThat(searchInputs.size(), is(0))
        );
    }

    @Test
    @DisplayName("should return only authorised workbasket input fields")
    void shouldReturnOnlyAuthorisedWorkbasketInputs() {
        doReturn(testWorkbasketInputs).when(getCriteriaOperation).execute(CASE_TYPE_ONE, CAN_READ, WORKBASKET);

        final List<WorkbasketInput> workbasketInputs = (List<WorkbasketInput>) classUnderTest.execute(CASE_TYPE_ONE, CAN_READ, WORKBASKET);

        assertAll(
            () -> assertThat(workbasketInputs.size(), is(3)),
            () -> assertThat(workbasketInputs.get(0), is(testWorkbasketInputs.get(0))),
            () -> assertThat(workbasketInputs.get(1), is(testWorkbasketInputs.get(1))),
            () -> assertThat(workbasketInputs.get(2), is(testWorkbasketInputs.get(2)))
        );
    }

    @Test
    @DisplayName("should return workbasket input field when user has necessary role")
    void shouldReturnWorkbasketInputWhenRoleExists() {
        doReturn(testWorkbasketInputs).when(getCriteriaOperation).execute(CASE_TYPE_ONE, CAN_READ, WORKBASKET);
        doReturn(Sets.newHashSet(ROLE1)).when(userRepository).getUserRoles();

        final List<WorkbasketInput> workbasketInputs = (List<WorkbasketInput>) classUnderTest.execute(CASE_TYPE_ONE, CAN_READ, WORKBASKET);

        assertAll(
            () -> assertThat(workbasketInputs.size(), is(4)),
            () -> assertThat(workbasketInputs.get(0), is(testWorkbasketInputs.get(0))),
            () -> assertThat(workbasketInputs.get(1), is(testWorkbasketInputs.get(1))),
            () -> assertThat(workbasketInputs.get(2), is(testWorkbasketInputs.get(2))),
            () -> assertThat(workbasketInputs.get(3), is(testWorkbasketInputs.get(4)))
        );
    }

    @Test
    @DisplayName("should not return return duplicate workbasket input field when user has more than necessary role")
    void shouldReturnDistinctWorkbasketInputWhenRoleExists() {
        doReturn(testWorkbasketInputs).when(getCriteriaOperation).execute(CASE_TYPE_ONE, CAN_READ, WORKBASKET);
        doReturn(Sets.newHashSet(ROLE1, ROLE2)).when(userRepository).getUserRoles();

        final List<WorkbasketInput> workbasketInputs = (List<WorkbasketInput>) classUnderTest.execute(CASE_TYPE_ONE, CAN_READ, WORKBASKET);

        assertAll(
            () -> assertThat(workbasketInputs.size(), is(4)),
            () -> assertThat(workbasketInputs.get(0), is(testWorkbasketInputs.get(0))),
            () -> assertThat(workbasketInputs.get(1), is(testWorkbasketInputs.get(1))),
            () -> assertThat(workbasketInputs.get(2), is(testWorkbasketInputs.get(2))),
            () -> assertThat(workbasketInputs.get(3), is(testWorkbasketInputs.get(4)))
        );
    }

    @Test
    @DisplayName("should return empty workbasket input list when no field is authorised")
    void shouldReturnEmptyWorkbasketInputWhenNoFieldIsAuthorised() {
        doReturn(new ArrayList<>()).when(getCriteriaOperation).execute(CASE_TYPE_ONE, CAN_READ, WORKBASKET);

        final List<WorkbasketInput> workbasketInputs = (List<WorkbasketInput>) classUnderTest.execute(CASE_TYPE_ONE, CAN_READ, WORKBASKET);

        assertAll(
            () -> assertThat(workbasketInputs.size(), is(0))
        );
    }

    @Test
    @DisplayName("should return workbasket input fields for complex fields")
    void shouldReturnWorkbasketInputForComplexFields() {
        testWorkbasketInputs = Arrays.asList(
            aWorkbasketInput().withFieldId(CASE_FIELD_ID_1_1).build(),
            aWorkbasketInput().withFieldId(CASE_FIELD_ID_1_2, "firstName").build(),
            aWorkbasketInput().withFieldId(CASE_FIELD_ID_1_2, "lastName").build()
        );

        doReturn(testWorkbasketInputs).when(getCriteriaOperation).execute(CASE_TYPE_ONE, CAN_READ, WORKBASKET);

        final List<WorkbasketInput> workbasketInputs = (List<WorkbasketInput>) classUnderTest.execute(CASE_TYPE_ONE, CAN_READ, WORKBASKET);

        assertAll(
            () -> assertThat(workbasketInputs.size(), is(3)),
            () -> assertThat(workbasketInputs.get(0), is(testWorkbasketInputs.get(0))),
            () -> assertThat(workbasketInputs.get(1), is(testWorkbasketInputs.get(1))),
            () -> assertThat(workbasketInputs.get(2), is(testWorkbasketInputs.get(2)))
        );
    }

    @Test
    @DisplayName("should return only authorised workbasket input fields for complex fields")
    void shouldReturnAuthorisedWorkbasketInputForComplexFields() {
        testWorkbasketInputs = Arrays.asList(
            aWorkbasketInput().withFieldId(CASE_FIELD_ID_1_1).build(),
            aWorkbasketInput().withFieldId(CASE_FIELD_ID_1_2, "firstName").withUserRole(ROLE1).build(),
            aWorkbasketInput().withFieldId(CASE_FIELD_ID_1_2, "lastName").withUserRole(ROLE2).build()
        );

        doReturn(testWorkbasketInputs).when(getCriteriaOperation).execute(CASE_TYPE_ONE, CAN_READ, WORKBASKET);
        doReturn(Sets.newHashSet(ROLE1)).when(userRepository).getUserRoles();

        final List<WorkbasketInput> workbasketInputs = (List<WorkbasketInput>) classUnderTest.execute(CASE_TYPE_ONE, CAN_READ, WORKBASKET);

        assertAll(
            () -> assertThat(workbasketInputs.size(), is(2)),
            () -> assertThat(workbasketInputs.get(0), is(testWorkbasketInputs.get(0))),
            () -> assertThat(workbasketInputs.get(1), is(testWorkbasketInputs.get(1)))
        );
    }

    @Test
    @DisplayName("should return search input fields for complex fields")
    void shouldReturnSearchInputForComplexFields() {
        testSearchInputs = Arrays.asList(
            aSearchInput().withFieldId(CASE_FIELD_ID_1_1).build(),
            aSearchInput().withFieldId(CASE_FIELD_ID_1_2, "firstName").build(),
            aSearchInput().withFieldId(CASE_FIELD_ID_1_2, "lastName").build()
        );

        doReturn(testSearchInputs).when(getCriteriaOperation).execute(CASE_TYPE_ONE, CAN_READ, WORKBASKET);

        final List<SearchInput> searchInputs = (List<SearchInput>) classUnderTest.execute(CASE_TYPE_ONE, CAN_READ, WORKBASKET);

        assertAll(
            () -> assertThat(searchInputs.size(), is(3)),
            () -> assertThat(searchInputs.get(0), is(testSearchInputs.get(0))),
            () -> assertThat(searchInputs.get(1), is(testSearchInputs.get(1))),
            () -> assertThat(searchInputs.get(2), is(testSearchInputs.get(2)))
        );
    }

    @Test
    @DisplayName("should return authorised search input fields for complex fields")
    void shouldReturnAuthorisedSearchInputForComplexFields() {
        testSearchInputs = Arrays.asList(
            aSearchInput().withFieldId(CASE_FIELD_ID_1_1).build(),
            aSearchInput().withFieldId(CASE_FIELD_ID_1_2, "firstName").withUserRole(ROLE1).build(),
            aSearchInput().withFieldId(CASE_FIELD_ID_1_2, "lastName").withUserRole(ROLE2).build()
        );

        doReturn(testSearchInputs).when(getCriteriaOperation).execute(CASE_TYPE_ONE, CAN_READ, WORKBASKET);
        doReturn(Sets.newHashSet(ROLE1)).when(userRepository).getUserRoles();

        final List<SearchInput> searchInputs = (List<SearchInput>) classUnderTest.execute(CASE_TYPE_ONE, CAN_READ, WORKBASKET);

        assertAll(
            () -> assertThat(searchInputs.size(), is(2)),
            () -> assertThat(searchInputs.get(0), is(testSearchInputs.get(0))),
            () -> assertThat(searchInputs.get(1), is(testSearchInputs.get(1)))
        );
    }
}
