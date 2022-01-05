package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.Classification;
import uk.gov.hmcts.ccd.domain.service.accessprofile.filter.BaseFilter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;


@SuppressWarnings({"ConstantConditions", "UnnecessaryLocalVariable"})
class BeginDateEndDateMatcherTest extends BaseFilter {

    private BeginDateEndDateMatcher classUnderTest;

    private final Instant beforeCurrentDate1 = Instant.now().minus(2, ChronoUnit.DAYS);
    private final Instant beforeCurrentDate2 = Instant.now().minus(1, ChronoUnit.DAYS);
    private final Instant afterCurrentDate1 = Instant.now().plus(1, ChronoUnit.DAYS);
    private final Instant afterCurrentDate2 = Instant.now().plus(2, ChronoUnit.DAYS);

    @BeforeEach
    void setUp() {
        classUnderTest = new BeginDateEndDateMatcher();
    }

    @Test
    void shouldMatchWhenBeginDateAndEndDateAreNull() {

        // GIVEN
        Instant beginDate = null;
        Instant endDate = null;

        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            beginDate, endDate, Classification.PRIVATE.name(), Optional.empty(), Optional.empty());

        // WHEN / THEN
        assertTrue(classUnderTest.matchAttribute(roleAssignment, mockCaseDetails()));
        assertTrue(classUnderTest.matchAttribute(roleAssignment, mockCaseTypeDefinition()));
    }

    @Test
    void shouldMatchWhenCurrentDateIsWithInBeginDateAndEndDate() {

        // GIVEN
        Instant beginDate = beforeCurrentDate1;
        Instant endDate = afterCurrentDate1;

        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            beginDate, endDate, Classification.PRIVATE.name(), Optional.empty(), Optional.empty());

        // WHEN / THEN
        assertTrue(classUnderTest.matchAttribute(roleAssignment, mockCaseDetails()));
        assertTrue(classUnderTest.matchAttribute(roleAssignment, mockCaseTypeDefinition()));
    }

    @Test
    void shouldMatchWhenBeginDateIsNullAndEndDateIsAfterCurrentDate() {

        // GIVEN
        Instant beginDate = null;
        Instant endDate = afterCurrentDate1;

        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_2, JURISDICTION_1,
            beginDate, endDate, Classification.PRIVATE.name(), Optional.empty(), Optional.empty());

        // WHEN / THEN
        assertTrue(classUnderTest.matchAttribute(roleAssignment, mockCaseDetails()));
        assertTrue(classUnderTest.matchAttribute(roleAssignment, mockCaseTypeDefinition()));
    }

    @Test
    void shouldMatchWhenEndDateIsNullAndBeginDateIsBeforeCurrentDate() {

        // GIVEN
        Instant beginDate = beforeCurrentDate1;
        Instant endDate = null;

        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_2, JURISDICTION_1,
            beginDate, endDate, Classification.PRIVATE.name(), Optional.empty(), Optional.empty());

        // WHEN / THEN
        assertTrue(classUnderTest.matchAttribute(roleAssignment, mockCaseDetails()));
        assertTrue(classUnderTest.matchAttribute(roleAssignment, mockCaseTypeDefinition()));
    }

    @Test
    void shouldNotMatchWhenBeginDateIsNullAndEndDateIsBeforeCurrentDate() {

        // GIVEN
        Instant beginDate = null;
        Instant endDate = beforeCurrentDate1;

        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_2, JURISDICTION_1,
            beginDate, endDate, Classification.PRIVATE.name(), Optional.empty(), Optional.empty());

        // WHEN / THEN
        assertFalse(classUnderTest.matchAttribute(roleAssignment, mockCaseDetails()));
        assertFalse(classUnderTest.matchAttribute(roleAssignment, mockCaseTypeDefinition()));
    }

    @Test
    void shouldNotMatchWhenEndDateIsNullAndBeginDateIsAfterCurrentDate() {

        // GIVEN
        Instant beginDate = afterCurrentDate1;
        Instant endDate = null;

        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_2, JURISDICTION_1,
            beginDate, endDate, Classification.PRIVATE.name(), Optional.empty(), Optional.empty());

        // WHEN / THEN
        assertFalse(classUnderTest.matchAttribute(roleAssignment, mockCaseDetails()));
        assertFalse(classUnderTest.matchAttribute(roleAssignment, mockCaseTypeDefinition()));
    }

    @Test
    void shouldNotMatchWhenBeginDateAndEndDateAreBeforeCurrentDate() {

        // GIVEN
        Instant beginDate = beforeCurrentDate1;
        Instant endDate = beforeCurrentDate2;

        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_2, JURISDICTION_1,
            beginDate, endDate, Classification.PRIVATE.name(), Optional.empty(), Optional.empty());

        // WHEN / THEN
        assertFalse(classUnderTest.matchAttribute(roleAssignment, mockCaseDetails()));
        assertFalse(classUnderTest.matchAttribute(roleAssignment, mockCaseTypeDefinition()));
    }

    @Test
    void shouldNotMatchWhenBeginDateAndEndDateAreAfterCurrentDate() {

        // GIVEN
        Instant beginDate = afterCurrentDate1;
        Instant endDate = afterCurrentDate2;

        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_2, JURISDICTION_1,
            beginDate, endDate, Classification.PRIVATE.name(), Optional.empty(), Optional.empty());

        // WHEN / THEN
        assertFalse(classUnderTest.matchAttribute(roleAssignment, mockCaseDetails()));
        assertFalse(classUnderTest.matchAttribute(roleAssignment, mockCaseTypeDefinition()));
    }

}
