package uk.gov.hmcts.ccd.domain.service.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseEventTrigger;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.*;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.AccessControlListBuilder.anAcl;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventBuilder.anCaseEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventTriggerBuilder.anEventTrigger;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.aCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseViewFieldBuilder.aViewField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseViewTriggerBuilder.aViewTrigger;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.WizardPageBuilder.aWizardPage;

class AccessControlServiceFilterTest {
    private static final String EVENT_ID_1 = "EVENT_ID_1";
    private static final String EVENT_ID_2 = "EVENT_ID_2";
    private static final String EVENT_ID_3 = "EVENT_ID_3";
    private static final CaseViewTrigger CASE_VIEW_TRIGGER_1 = aViewTrigger().withId(EVENT_ID_1).build();
    private static final CaseViewTrigger CASE_VIEW_TRIGGER_2 = aViewTrigger().withId(EVENT_ID_2).build();
    private static final CaseViewTrigger CASE_VIEW_TRIGGER_3 = aViewTrigger().withId(EVENT_ID_3).build();

    private final CaseViewTrigger[] caseViewTriggers = {CASE_VIEW_TRIGGER_1, CASE_VIEW_TRIGGER_2, CASE_VIEW_TRIGGER_3};
    private AccessControlService accessControlService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);


        accessControlService = new AccessControlService();
    }

    @Test
    @DisplayName("Should filter caseFields if CREATE ACL is missing for some fields")
    void filterCaseFieldsUserHasAccess() {
        final CaseType caseType = newCaseType()
            .withField(aCaseField()
                .withId("Name")
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(false)
                    .build())
                .build())
            .withField(aCaseField()
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

        CaseEventTrigger caseEventTrigger = anEventTrigger()
            .withField(caseViewField1)
            .withField(caseViewField2)
            .withWizardPage(aWizardPage()
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
            .withField(aCaseField()
                .withId("Name")
                .withAcl(anAcl()
                    .withRole(ROLE_NOT_IN_USER_ROLES)
                    .withCreate(true)
                    .build())
                .build())
            .withField(aCaseField()
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

        CaseEventTrigger caseEventTrigger = anEventTrigger()
            .withField(caseViewField1)
            .withField(caseViewField2)
            .withWizardPage(aWizardPage()
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
            .withField(aCaseField()
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

        CaseEventTrigger caseEventTrigger = anEventTrigger()
            .withField(caseViewField1)
            .withField(caseViewField2)
            .withWizardPage(aWizardPage()
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
    @DisplayName("Should not change view trigger when all has required ACL")
    void doNotFilterCaseViewTriggersWhenACLsMatch() {
        final CaseEvent event1 = anCaseEvent()
            .withId(EVENT_ID_1)
            .withAcl(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .build()).build();
        final CaseEvent event2 = anCaseEvent()
            .withId(EVENT_ID_2)
            .withAcl(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .build()).build();
        final CaseEvent event3 = anCaseEvent()
            .withId(EVENT_ID_3)
            .withAcl(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .build()).build();
        final List<CaseEvent> caseEventDefinitions = Arrays.asList(event1, event2, event3);

        final CaseViewTrigger[] filteredTriggers = accessControlService.filterCaseViewTriggersByCreateAccess(this
            .caseViewTriggers, caseEventDefinitions, USER_ROLES);
        assertArrayEquals(this.caseViewTriggers, filteredTriggers);
    }

    @Test
    @DisplayName("Should filter view triggers according to the ACLs")
    void filterCaseViewTriggersWhenCreateACLIsMissing() {
        final CaseEvent event1 = anCaseEvent()
            .withId(EVENT_ID_1)
            .withAcl(anAcl()
                .withRole(ROLE_NOT_IN_USER_ROLES)
                .withCreate(true)
                .build()).build();
        final CaseEvent event2 = anCaseEvent()
            .withId(EVENT_ID_2)
            .withAcl(anAcl()
                .withRole(ROLE_NOT_IN_USER_ROLES_2)
                .withCreate(true)
                .build()).build();
        final CaseEvent event3 = anCaseEvent()
            .withId(EVENT_ID_3)
            .withAcl(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .build()).build();
        final List<CaseEvent> caseEventDefinitions = Arrays.asList(event1, event2, event3);

        final CaseViewTrigger[] filteredTriggers = accessControlService.filterCaseViewTriggersByCreateAccess(this
            .caseViewTriggers, caseEventDefinitions, USER_ROLES);
        assertAll(
            () -> assertThat(filteredTriggers.length, is(1)),
            () -> assertThat(filteredTriggers[0], is(CASE_VIEW_TRIGGER_3))
        );
    }

    @Test
    @DisplayName("Should not filter and case field if user has all required ACLs")
    void doNotFilterCaseFieldsIfUserHasAccess() {
        final CaseField caseField1 = aCaseField()
            .withId("Firsname")
            .withAcl(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withRead(true)
                .build())
            .build();
        final CaseField caseField2 = aCaseField()
            .withId("LastName")
            .withAcl(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withRead(true)
                .build())
            .build();
        final CaseField caseField3 = aCaseField()
            .withId("Address")
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
        final CaseField caseField1 = aCaseField()
            .withId("Firsname")
            .withAcl(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withRead(true)
                .build())
            .build();
        final CaseField caseField2 = aCaseField()
            .withId("LastName")
            .withAcl(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withRead(true)
                .build())
            .build();
        final CaseField caseField3 = aCaseField()
            .withId("Address")
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
