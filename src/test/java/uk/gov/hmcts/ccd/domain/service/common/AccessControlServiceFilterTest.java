package uk.gov.hmcts.ccd.domain.service.common;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_UPDATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.*;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.AccessControlListBuilder.anAcl;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventBuilder.newCaseEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventTriggerBuilder.newCaseEventTrigger;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseViewFieldBuilder.aViewField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseViewTriggerBuilder.aViewTrigger;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.ComplexACLBuilder.aComplexACL;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.WizardPageBuilder.newWizardPage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import uk.gov.hmcts.ccd.domain.model.aggregated.CaseEventTrigger;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;

class AccessControlServiceFilterTest {
    private static final String EVENT_ID_1 = "EVENT_ID_1";
    private static final String EVENT_ID_2 = "EVENT_ID_2";
    private static final String EVENT_ID_3 = "EVENT_ID_3";
    private static final CaseViewTrigger CASE_VIEW_TRIGGER_1 = aViewTrigger().withId(EVENT_ID_1).build();
    private static final CaseViewTrigger CASE_VIEW_TRIGGER_2 = aViewTrigger().withId(EVENT_ID_2).build();
    private static final CaseViewTrigger CASE_VIEW_TRIGGER_3 = aViewTrigger().withId(EVENT_ID_3).build();

    final CaseViewTrigger[] caseViewTriggers = {CASE_VIEW_TRIGGER_1, CASE_VIEW_TRIGGER_2, CASE_VIEW_TRIGGER_3};
    AccessControlService accessControlService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        accessControlService = new AccessControlService(new CompoundAccessControlService());
    }

    @Nested
    @DisplayName("FilterCaseViewTriggersTests")
    class FilterCaseViewTriggersTests {

        @Test
        @DisplayName("Should not change view trigger when all has required ACL")
        void doNotFilterCaseViewTriggersWhenACLsMatch() {
            final CaseEvent event1 = newCaseEvent()
                .withId(EVENT_ID_1)
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build()).build();
            final CaseEvent event2 = newCaseEvent()
                .withId(EVENT_ID_2)
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build()).build();
            final CaseEvent event3 = newCaseEvent()
                .withId(EVENT_ID_3)
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build()).build();
            final List<CaseEvent> caseEventDefinitions = Arrays.asList(event1, event2, event3);

            final CaseViewTrigger[] filteredTriggers = accessControlService.filterCaseViewTriggersByCreateAccess(
                caseViewTriggers, caseEventDefinitions, USER_ROLES);
            assertArrayEquals(caseViewTriggers, filteredTriggers);
        }

        @Test
        @DisplayName("Should filter view triggers according to the ACLs")
        void filterCaseViewTriggersWhenCreateACLIsMissing() {
            final CaseEvent event1 = newCaseEvent()
                .withId(EVENT_ID_1)
                .withAcl(anAcl()
                    .withRole(ROLE_NOT_IN_USER_ROLES)
                    .withCreate(true)
                    .build()).build();
            final CaseEvent event2 = newCaseEvent()
                .withId(EVENT_ID_2)
                .withAcl(anAcl()
                    .withRole(ROLE_NOT_IN_USER_ROLES_2)
                    .withCreate(true)
                    .build()).build();
            final CaseEvent event3 = newCaseEvent()
                .withId(EVENT_ID_3)
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build()).build();
            final List<CaseEvent> caseEventDefinitions = Arrays.asList(event1, event2, event3);

            final CaseViewTrigger[] filteredTriggers = accessControlService.filterCaseViewTriggersByCreateAccess(caseViewTriggers, caseEventDefinitions, USER_ROLES);
            assertAll(
                () -> assertThat(filteredTriggers.length, is(1)),
                () -> assertThat(filteredTriggers[0], is(CASE_VIEW_TRIGGER_3))
            );
        }
    }

    @Nested
    @DisplayName("filterCaseFieldsByAccess Tests - Simple CaseFields")
    class FilterCaseFieldsByAccessSimpleFieldTests {
        @Test
        @DisplayName("Should filter caseFields if CREATE ACL is missing for some fields")
        void filterCaseFieldsUserHasAccess() {
            final CaseType caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Name")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(false)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("Surname")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_2)
                        .withCreate(true)
                        .build())
                    .build())
                .build();

            final CaseViewField caseViewField1 = aViewField()
                .withId("Name")
                .build();

            final CaseViewField caseViewField2 = aViewField()
                .withId("Surname")
                .build();

            CaseEventTrigger caseEventTrigger = newCaseEventTrigger()
                .withField(caseViewField1)
                .withField(caseViewField2)
                .withWizardPage(newWizardPage()
                    .withId("Page One")
                    .withField(caseViewField1)
                    .withField(caseViewField2)
                    .build()
                )
                .build();

            CaseEventTrigger eventTrigger = accessControlService.filterCaseViewFieldsByAccess(
                caseEventTrigger,
                caseType.getCaseFields(),
                USER_ROLES,
                CAN_CREATE);

            assertAll(
                () -> assertThat(eventTrigger.getCaseFields(), hasSize(1)),
                () -> assertThat(eventTrigger.getCaseFields(), hasItem(caseViewField2))
            );
        }

        @Test
        @DisplayName("Should filter all caseFields if CREATE ACL is missing")
        void filterCaseFieldsUserHasNoAccess() {
            final CaseType caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Name")
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("Surname")
                    .withAcl(anAcl()
                        .withRole(ROLE_NOT_IN_USER_ROLES_2)
                        .withCreate(true)
                        .build())
                    .build())
                .build();

            final CaseViewField caseViewField1 = aViewField()
                .withId("Name")
                .build();

            final CaseViewField caseViewField2 = aViewField()
                .withId("Surname")
                .build();

            CaseEventTrigger caseEventTrigger = newCaseEventTrigger()
                .withField(caseViewField1)
                .withField(caseViewField2)
                .withWizardPage(newWizardPage()
                    .withId("Page One")
                    .withField(caseViewField1)
                    .withField(caseViewField2)
                    .build()
                )
                .build();


            CaseEventTrigger eventTrigger = accessControlService.filterCaseViewFieldsByAccess(
                caseEventTrigger,
                caseType.getCaseFields(),
                USER_ROLES,
                CAN_CREATE);

            assertThat(eventTrigger.getCaseFields(), hasSize(0));
        }

        @Test
        @DisplayName("Should filter caseFields definition is missing for those fields")
        void filterCaseFieldsWithNoDefinition() {
            final CaseType caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Surname")
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_2)
                        .withCreate(true)
                        .build())
                    .build())
                .build();

            final CaseViewField caseViewField1 = aViewField()
                .withId("Name")
                .build();
            final CaseViewField caseViewField2 = aViewField()
                .withId("Surname")
                .build();

            CaseEventTrigger caseEventTrigger = newCaseEventTrigger()
                .withField(caseViewField1)
                .withField(caseViewField2)
                .withWizardPage(newWizardPage()
                    .withId("Page One")
                    .withField(caseViewField1)
                    .withField(caseViewField2)
                    .build()
                )
                .build();

            CaseEventTrigger eventTrigger = accessControlService.filterCaseViewFieldsByAccess(
                caseEventTrigger,
                caseType.getCaseFields(),
                USER_ROLES,
                CAN_CREATE);

            assertAll(
                () -> assertThat(eventTrigger.getCaseFields(), hasSize(1)),
                () -> assertThat(eventTrigger.getCaseFields(), hasItem(caseViewField2))
            );
        }

        @Test
        @DisplayName("Should not filter and case field if user has all required ACLs")
        void doNotFilterCaseFieldsIfUserHasAccess() {
            final CaseField caseField1 = newCaseField()
                .withId("FirstName")
                .withFieldType(aFieldType()
                    .withId("Text")
                    .withType("Text")
                    .build())
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(true)
                    .build())
                .build();
            final CaseField caseField2 = newCaseField()
                .withId("LastName")
                .withFieldType(aFieldType()
                    .withId("Text")
                    .withType("Text")
                    .build())
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(true)
                    .build())
                .build();
            final CaseField caseField3 = newCaseField()
                .withId("Address")
                .withFieldType(aFieldType()
                    .withId("Text")
                    .withType("Text")
                    .build())
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(true)
                    .build())
                .build();
            List<CaseField> caseFieldDefinitions = Arrays.asList(caseField1, caseField2, caseField3);

            final List<CaseField> filteredCaseFields = accessControlService.filterCaseFieldsByAccess(caseFieldDefinitions,
                USER_ROLES, CAN_READ);
            assertAll(
                () -> assertThat(filteredCaseFields, hasSize(3)),
                () -> assertThat(filteredCaseFields, hasItem(caseField1)),
                () -> assertThat(filteredCaseFields, hasItem(caseField2)),
                () -> assertThat(filteredCaseFields, hasItem(caseField3))
            );
        }

        @Test
        @DisplayName("Should filter and case fields if user missing ACLs")
        void filterCaseFieldsByUserAccess() {
            final CaseField caseField1 = newCaseField()
                .withId("FirstName")
                .withFieldType(aFieldType()
                    .withId("Text")
                    .withType("Text")
                    .build())
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(true)
                    .build())
                .build();
            final CaseField caseField2 = newCaseField()
                .withId("LastName")
                .withFieldType(aFieldType()
                    .withId("Text")
                    .withType("Text")
                    .build())
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(true)
                    .build())
                .build();
            final CaseField caseField3 = newCaseField()
                .withId("Address")
                .withFieldType(aFieldType()
                    .withId("Text")
                    .withType("Text")
                    .build())
                .withAcl(anAcl()
                    .withRole(ROLE_NOT_IN_USER_ROLES)
                    .withRead(true)
                    .build())
                .build();
            List<CaseField> caseFieldDefinitions = Arrays.asList(caseField1, caseField2, caseField3);

            final List<CaseField> filteredCaseFields = accessControlService.filterCaseFieldsByAccess(caseFieldDefinitions,
                USER_ROLES, CAN_READ);
            assertAll(
                () -> assertThat(filteredCaseFields, hasSize(2)),
                () -> assertThat(filteredCaseFields, hasItem(caseField1)),
                () -> assertThat(filteredCaseFields, hasItem(caseField2)),
                () -> assertThat(filteredCaseFields, not(hasItem(caseField3)))
            );
        }
    }

    @Nested
    @DisplayName("filterCaseFieldsByAccess Tests - Compound CaseFields")
    class FilterCaseFieldsByAccessCompoundFieldTests {
        @Test
        @DisplayName("Should filter sub fields of caseFields based on Complex ACLs on READ")
        void filterCaseFieldsUserHasReadAccess() {
            final CaseField people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withRead(true)
                .build()));
            people.setComplexACLs(asList(
                aComplexACL()
                    .withListElementCode("FirstName")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("LastName")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Line1")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Line2")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withRead(false)
                    .build()
            ));
            people.propagateACLsToNestedFields();

            final List<CaseField> filteredCaseFields = accessControlService.filterCaseFieldsByAccess(asList(people), USER_ROLES, CAN_READ);

            assertAll(
                () -> assertThat(filteredCaseFields, hasSize(1)),
                () -> assertThat(filteredCaseFields.get(0).getFieldType().getChildren(), hasSize(3)),
                () -> assertThat(filteredCaseFields.get(0).getFieldType().getChildren().get(2).getId(), is("Addresses")),
                () -> assertThat(filteredCaseFields.get(0).getFieldType().getChildren().get(2).getFieldType().getChildren().size(), is(1)),
                () -> assertThat(filteredCaseFields.get(0).getFieldType().getChildren().get(2).getFieldType().getChildren().get(0).getFieldType().getChildren().size(), is(1))
            );
        }

        @Test
        @DisplayName("Should filter sub fields of caseFields based on Complex ACLs on UPDATE")
        void filterCaseFieldsUserHasUpdateAccess() {
            final CaseField people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withUpdate(true)
                .build()));
            people.setComplexACLs(asList(
                aComplexACL()
                    .withListElementCode("FirstName")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("LastName")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Line1")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Line2")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(false)
                    .build()
            ));
            people.propagateACLsToNestedFields();

            final List<CaseField> filteredCaseFields = accessControlService.filterCaseFieldsByAccess(asList(people), USER_ROLES, CAN_UPDATE);

            assertAll(
                () -> assertThat(filteredCaseFields, hasSize(1)),
                () -> assertThat(filteredCaseFields.get(0).getFieldType().getChildren(), hasSize(3)),
                () -> assertThat(filteredCaseFields.get(0).getFieldType().getChildren().get(2).getId(), is("Addresses")),
                () -> assertThat(filteredCaseFields.get(0).getFieldType().getChildren().get(2).getFieldType().getChildren().size(), is(1)),
                () -> assertThat(filteredCaseFields.get(0).getFieldType().getChildren().get(2).getFieldType().getChildren().get(0).getFieldType().getChildren().size(), is(1))
            );
        }
    }
}
