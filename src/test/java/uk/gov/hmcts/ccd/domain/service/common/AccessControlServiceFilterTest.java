package uk.gov.hmcts.ccd.domain.service.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseUpdateViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewActionableEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.PREDEFINED_COMPLEX_ADDRESS_UK;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_UPDATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.ROLE_IN_USER_ROLES;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.ROLE_IN_USER_ROLES_2;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.ROLE_NOT_IN_USER_ROLES;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.ROLE_NOT_IN_USER_ROLES_2;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.USER_ROLES;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.getAddressFieldType;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.getPeopleCollectionFieldDefinition;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceTest.getPersonFieldType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.AccessControlListBuilder.anAcl;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventBuilder.newCaseEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseUpdateViewEventBuilder.newCaseUpdateViewEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseViewActionableEventBuilder.aViewTrigger;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseViewFieldBuilder.aViewField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.ComplexACLBuilder.aComplexACL;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.WizardPageBuilder.newWizardPage;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.WizardPageComplexFieldOverrideBuilder.newWizardPageComplexFieldOverride;

class AccessControlServiceFilterTest {
    private static final String EVENT_ID_1 = "EVENT_ID_1";
    private static final String EVENT_ID_2 = "EVENT_ID_2";
    private static final String EVENT_ID_3 = "EVENT_ID_3";
    private static final CaseViewActionableEvent CASE_VIEW_TRIGGER_1 = aViewTrigger().withId(EVENT_ID_1).build();
    private static final CaseViewActionableEvent CASE_VIEW_TRIGGER_2 = aViewTrigger().withId(EVENT_ID_2).build();
    private static final CaseViewActionableEvent CASE_VIEW_TRIGGER_3 = aViewTrigger().withId(EVENT_ID_3).build();

    final CaseViewActionableEvent[] caseViewTriggers = {CASE_VIEW_TRIGGER_1, CASE_VIEW_TRIGGER_2, CASE_VIEW_TRIGGER_3};
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
            final CaseEventDefinition event1 = newCaseEvent()
                .withId(EVENT_ID_1)
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build()).build();
            final CaseEventDefinition event2 = newCaseEvent()
                .withId(EVENT_ID_2)
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build()).build();
            final CaseEventDefinition event3 = newCaseEvent()
                .withId(EVENT_ID_3)
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build()).build();
            final List<CaseEventDefinition> caseEventDefinitions = Arrays.asList(event1, event2, event3);

            final CaseViewActionableEvent[] filteredTriggers = accessControlService.filterCaseViewTriggersByCreateAccess(
                caseViewTriggers, caseEventDefinitions, USER_ROLES);
            assertArrayEquals(caseViewTriggers, filteredTriggers);
        }

        @Test
        @DisplayName("Should filter view triggers according to the ACLs")
        void filterCaseViewTriggersWhenCreateACLIsMissing() {
            final CaseEventDefinition event1 = newCaseEvent()
                .withId(EVENT_ID_1)
                .withAcl(anAcl()
                    .withRole(ROLE_NOT_IN_USER_ROLES)
                    .withCreate(true)
                    .build()).build();
            final CaseEventDefinition event2 = newCaseEvent()
                .withId(EVENT_ID_2)
                .withAcl(anAcl()
                    .withRole(ROLE_NOT_IN_USER_ROLES_2)
                    .withCreate(true)
                    .build()).build();
            final CaseEventDefinition event3 = newCaseEvent()
                .withId(EVENT_ID_3)
                .withAcl(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build()).build();
            final List<CaseEventDefinition> caseEventDefinitions = Arrays.asList(event1, event2, event3);

            final CaseViewActionableEvent[] filteredTriggers =
                accessControlService.filterCaseViewTriggersByCreateAccess(caseViewTriggers, caseEventDefinitions, USER_ROLES);
            assertAll(
                () -> assertThat(filteredTriggers.length, is(1)),
                () -> assertThat(filteredTriggers[0], is(CASE_VIEW_TRIGGER_3))
            );
        }
    }

    @Nested
    @DisplayName("FilterCaseViewFields for Event Triggers Tests - Simple CaseFields")
    class FilterCaseViewFieldsByAccessSimpleFieldTests {
        @Test
        @DisplayName("Should filter caseFields if CREATE ACL is missing for some fields")
        void filterCaseFieldsUserHasAccess() {
            final CaseTypeDefinition caseType = newCaseType()
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

            CaseUpdateViewEvent caseEventTrigger = newCaseUpdateViewEvent()
                .withField(caseViewField1)
                .withField(caseViewField2)
                .withWizardPage(newWizardPage()
                    .withId("Page One")
                    .withField(caseViewField1)
                    .withField(caseViewField2)
                    .build()
                )
                .build();

            CaseUpdateViewEvent eventTrigger = accessControlService.filterCaseViewFieldsByAccess(
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
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
            final CaseTypeDefinition caseType = newCaseType()
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

            CaseUpdateViewEvent caseEventTrigger = newCaseUpdateViewEvent()
                .withField(caseViewField1)
                .withField(caseViewField2)
                .withWizardPage(newWizardPage()
                    .withId("Page One")
                    .withField(caseViewField1)
                    .withField(caseViewField2)
                    .build()
                )
                .build();


            CaseUpdateViewEvent eventTrigger = accessControlService.filterCaseViewFieldsByAccess(
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_CREATE);

            assertThat(eventTrigger.getCaseFields(), hasSize(0));
        }

        @Test
        @DisplayName("Should filter caseFields definition is missing for those fields")
        void filterCaseFieldsWithNoDefinition() {
            final CaseTypeDefinition caseType = newCaseType()
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

            CaseUpdateViewEvent caseEventTrigger = newCaseUpdateViewEvent()
                .withField(caseViewField1)
                .withField(caseViewField2)
                .withWizardPage(newWizardPage()
                    .withId("Page One")
                    .withField(caseViewField1)
                    .withField(caseViewField2)
                    .build()
                )
                .build();

            CaseUpdateViewEvent eventTrigger = accessControlService.filterCaseViewFieldsByAccess(
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_CREATE);

            assertAll(
                () -> assertThat(eventTrigger.getCaseFields(), hasSize(1)),
                () -> assertThat(eventTrigger.getCaseFields(), hasItem(caseViewField2))
            );
        }
    }

    @Nested
    @DisplayName("FilterCaseFields for Event Triggers Tests - Compound CaseFields")
    class FilterCaseViewFieldsByAccessCompoundFieldTests {
        @Test
        @DisplayName("Should filter child fields of a complex caseField if CREATE ACL is missing for child fields")
        void filterComplexCaseFieldChildrenByCreateAccess() {
            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Name")
                    .withFieldType(aFieldType()
                        .withId("Text")
                        .withType("Text")
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(false)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("Surname")
                    .withFieldType(aFieldType()
                        .withId("Text")
                        .withType("Text")
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_2)
                        .withCreate(true)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("BornAddress")
                    .withFieldType(getAddressFieldType())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .withComplexACL(aComplexACL()
                        .withListElementCode("Name")
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(false)
                        .build())
                    .withComplexACL(aComplexACL()
                        .withListElementCode("Address")
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .withComplexACL(aComplexACL()
                        .withListElementCode("Address.Line1")
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .withComplexACL(aComplexACL()
                        .withListElementCode("Address.PostCode")
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(true)
                        .build())
                    .build())
                .build();
            caseType.getCaseFieldDefinitions().stream().forEach(caseField -> caseField.propagateACLsToNestedFields());

            final CaseViewField caseViewField1 = aViewField()
                .withId("Name")
                .build();
            final CaseViewField caseViewField2 = aViewField()
                .withId("Surname")
                .build();
            final CaseViewField caseViewField3 = aViewField()
                .withId("BornAddress")
                .withFieldType(getAddressFieldType())
                .build();

            CaseUpdateViewEvent caseEventTrigger = newCaseUpdateViewEvent()
                .withField(caseViewField1)
                .withField(caseViewField2)
                .withField(caseViewField3)
                .withWizardPage(newWizardPage()
                    .withId("Page One")
                    .withField(caseViewField1)
                    .withField(caseViewField2)
                    .withField(caseViewField3)
                    .build()
                )
                .build();

            CaseUpdateViewEvent eventTrigger = accessControlService.filterCaseViewFieldsByAccess(
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_CREATE);

            assertAll(
                () -> assertThat(eventTrigger.getCaseFields(), hasSize(2)),
                () -> assertThat(eventTrigger.getCaseFields(), hasItem(caseViewField2)),
                () -> assertThat(eventTrigger.getCaseFields().get(1).getId(), is("BornAddress")),
                () -> assertThat(eventTrigger.getCaseFields().get(1).getFieldTypeDefinition().getChildren().size(), is(1)),
                () -> assertThat(eventTrigger.getCaseFields().get(1).getFieldTypeDefinition().getChildren().get(0).getId(), is("Address")),
                () -> assertThat(eventTrigger.getCaseFields().get(1).getFieldTypeDefinition()
                    .getChildren()
                    .get(0)
                    .getFieldTypeDefinition()
                    .getChildren()
                    .size(), is(2)),
                () -> assertThat(
                    eventTrigger.getCaseFields().get(1).getFieldTypeDefinition().getChildren().get(0).getFieldTypeDefinition().getChildren().get(0).getId(),
                    is("Line1")
                ),
                () -> assertThat(
                    eventTrigger.getCaseFields().get(1).getFieldTypeDefinition().getChildren().get(0).getFieldTypeDefinition().getChildren().get(1).getId(),
                    is("PostCode")
                )
            );
        }

        @Test
        @DisplayName("Should filter child fields of a collection caseField if UPDATE ACL is missing for child fields")
        void filterCollectionCaseFieldChildrenByUpdateAccess() {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withUpdate(true)
                .build()));
            people.setComplexACLs(asList(
                aComplexACL()
                    .withListElementCode("LastName")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("BirthInfo")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Notes")
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
                    .withUpdate(true)
                    .build()
            ));

            final CaseTypeDefinition caseType = newCaseType()
                .withField(people)
                .withField(newCaseField()
                    .withId("Name")
                    .withFieldType(aFieldType()
                        .withId("Text")
                        .withType("Text")
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withUpdate(false)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("Surname")
                    .withFieldType(aFieldType()
                        .withId("Text")
                        .withType("Text")
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_2)
                        .withUpdate(true)
                        .build())
                    .build())
                .build();
            caseType.getCaseFieldDefinitions().stream().forEach(caseField -> caseField.propagateACLsToNestedFields());

            final CaseViewField caseViewField1 = aViewField()
                .withId("Name")
                .build();
            final CaseViewField caseViewField2 = aViewField()
                .withId("Surname")
                .build();
            final CaseViewField caseViewField3 = aViewField()
                .withId("People")
                .withFieldType(aFieldType()
                    .withId("G339483948")
                    .withType(COLLECTION)
                    .build())
                .build();
            caseViewField3.getFieldTypeDefinition().setCollectionFieldTypeDefinition(getPersonFieldType());

            CaseUpdateViewEvent caseEventTrigger = newCaseUpdateViewEvent()
                .withField(caseViewField1)
                .withField(caseViewField2)
                .withField(caseViewField3)
                .withWizardPage(newWizardPage()
                    .withId("Page One")
                    .withField(caseViewField1)
                    .withField(caseViewField2)
                    .withField(caseViewField3)
                    .build()
                )
                .build();

            CaseUpdateViewEvent eventTrigger = accessControlService.filterCaseViewFieldsByAccess(
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_UPDATE);

            assertAll(
                () -> assertThat(eventTrigger.getCaseFields(), hasSize(2)),
                () -> assertThat(eventTrigger.getCaseFields(), not(hasItem(caseViewField1))),
                () -> assertThat(eventTrigger.getCaseFields(), hasItem(caseViewField2)),
                () -> assertThat(eventTrigger.getCaseFields().get(1).getId(), is("People")),
                () -> assertThat(eventTrigger.getCaseFields().get(1).getFieldTypeDefinition().getChildren().size(), is(4)),
                () -> assertThat(eventTrigger.getCaseFields().get(1).getFieldTypeDefinition().getChildren().get(0).getId(), is("LastName")),
                () -> assertThat(
                    eventTrigger.getCaseFields().get(1).getFieldTypeDefinition().getChildren().get(2).getFieldTypeDefinition().getChildren().size(),
                    is(1)
                ),
                () -> assertThat(
                    eventTrigger.getCaseFields()
                        .get(1)
                        .getFieldTypeDefinition()
                        .getChildren().get(2)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(0)
                        .getId(),
                    is("Address")
                ),
                () -> assertThat(
                    eventTrigger.getCaseFields()
                        .get(1)
                        .getFieldTypeDefinition()
                        .getChildren().get(2)
                        .getFieldTypeDefinition()
                        .getChildren().get(0)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .size(),
                    is(2)
                ),
                () -> assertThat(
                    eventTrigger.getCaseFields()
                        .get(1)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(2)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(0)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(0).getId(),
                    is("Line1")
                ),
                () -> assertThat(
                    eventTrigger.getCaseFields()
                        .get(1)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(2)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(0)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(1).getId(),
                    is("Line2")
                )
            );
        }

        @Test
        @DisplayName("Should filter child fields of a collection caseField if UPDATE ACL is missing for child fields - alternate")
        void filterCollectionCaseFieldChildrenByUpdateAccessAlternate() {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withUpdate(true)
                .build()));
            people.setComplexACLs(asList(
                aComplexACL()
                    .withListElementCode("LastName")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("BirthInfo")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Notes")
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
                    .withUpdate(false)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Line1")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(false)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Line2")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withUpdate(false)
                    .build()
            ));

            final CaseTypeDefinition caseType = newCaseType()
                .withField(people)
                .withField(newCaseField()
                    .withId("Name")
                    .withFieldType(aFieldType()
                        .withId("Text")
                        .withType("Text")
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withUpdate(false)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("Surname")
                    .withFieldType(aFieldType()
                        .withId("Text")
                        .withType("Text")
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_2)
                        .withUpdate(true)
                        .build())
                    .build())
                .build();
            caseType.getCaseFieldDefinitions().stream().forEach(caseField -> caseField.propagateACLsToNestedFields());

            final CaseViewField caseViewField1 = aViewField()
                .withId("Name")
                .build();
            final CaseViewField caseViewField2 = aViewField()
                .withId("Surname")
                .build();
            final CaseViewField caseViewField3 = aViewField()
                .withId("People")
                .withFieldType(aFieldType()
                    .withId("G339483948")
                    .withType(COLLECTION)
                    .build())
                .build();
            caseViewField3.getFieldTypeDefinition().setCollectionFieldTypeDefinition(getPersonFieldType());

            CaseUpdateViewEvent caseEventTrigger = newCaseUpdateViewEvent()
                .withField(caseViewField1)
                .withField(caseViewField2)
                .withField(caseViewField3)
                .withWizardPage(newWizardPage()
                    .withId("Page One")
                    .withField(caseViewField1)
                    .withField(caseViewField2)
                    .withField(caseViewField3)
                    .build()
                )
                .build();

            CaseUpdateViewEvent eventTrigger = accessControlService.filterCaseViewFieldsByAccess(
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_UPDATE);

            assertAll(
                () -> assertThat(eventTrigger.getCaseFields(), hasSize(2)),
                () -> assertThat(eventTrigger.getCaseFields(), not(hasItem(caseViewField1))),
                () -> assertThat(eventTrigger.getCaseFields(), hasItem(caseViewField2)),
                () -> assertThat(eventTrigger.getCaseFields().get(1).getId(), is("People")),
                () -> assertThat(eventTrigger.getCaseFields().get(1).getFieldTypeDefinition().getChildren().size(), is(4)),
                () -> assertThat(eventTrigger.getCaseFields().get(1).getFieldTypeDefinition().getChildren().get(0).getId(), is("LastName")),
                () -> assertThat(eventTrigger.getCaseFields().get(1)
                    .getFieldTypeDefinition()
                    .getChildren()
                    .get(2)
                    .getFieldTypeDefinition()
                    .getChildren().size(), is(0))
            );
        }

        @Test
        @DisplayName("Should filter child fields of a collection caseField if CREATE ACL is missing for child fields")
        void filterCollectionCaseFieldChildrenByCreateAccess() {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .build()));
            people.setComplexACLs(asList(
                aComplexACL()
                    .withListElementCode("LastName")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("BirthInfo")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("BirthInfo.BornCity")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("BirthInfo.BornCountry")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(false)
                    .build(),
                aComplexACL()
                    .withListElementCode("BirthInfo.BornAddress")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("BirthInfo.BornAddress.Name")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(false)
                    .build(),
                aComplexACL()
                    .withListElementCode("BirthInfo.BornAddress.Address")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("BirthInfo.BornAddress.Address.Line1")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("BirthInfo.BornAddress.Address.Country")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Notes")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Notes.Txt")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(false)
                    .build(),
                aComplexACL()
                    .withListElementCode("Notes.Tags")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Notes.Tags.Tag")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Line1")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Line2")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build()
            ));

            final CaseTypeDefinition caseType = newCaseType()
                .withField(people)
                .withField(newCaseField()
                    .withId("Name")
                    .withFieldType(aFieldType()
                        .withId("Text")
                        .withType("Text")
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(false)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("Surname")
                    .withFieldType(aFieldType()
                        .withId("Text")
                        .withType("Text")
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_2)
                        .withCreate(true)
                        .build())
                    .build())
                .build();
            caseType.getCaseFieldDefinitions().stream().forEach(caseField -> caseField.propagateACLsToNestedFields());

            final CaseViewField caseViewField1 = aViewField()
                .withId("Name")
                .build();
            final CaseViewField caseViewField2 = aViewField()
                .withId("Surname")
                .build();
            final CaseViewField caseViewField3 = aViewField()
                .withId("People")
                .withFieldType(aFieldType()
                    .withId("G339483948")
                    .withType(COLLECTION)
                    .build())
                .build();
            caseViewField3.getFieldTypeDefinition().setCollectionFieldTypeDefinition(getPersonFieldType());

            CaseUpdateViewEvent caseEventTrigger = newCaseUpdateViewEvent()
                .withField(caseViewField1)
                .withField(caseViewField2)
                .withField(caseViewField3)
                .withWizardPage(newWizardPage()
                    .withId("Page One")
                    .withField(caseViewField1)
                    .withField(caseViewField2)
                    .withField(caseViewField3, asList(
                        newWizardPageComplexFieldOverride()
                            .withComplexFieldId("People.FirstName")
                            .build(),
                        newWizardPageComplexFieldOverride()
                            .withComplexFieldId("People.LastName")
                            .build(),
                        newWizardPageComplexFieldOverride()
                            .withComplexFieldId("People.BirthInfo.BornCity")
                            .build(),
                        newWizardPageComplexFieldOverride()
                            .withComplexFieldId("People.BirthInfo.BornCountry")
                            .build(),
                        newWizardPageComplexFieldOverride()
                            .withComplexFieldId("People.BirthInfo.BornAddress.Name")
                            .build(),
                        newWizardPageComplexFieldOverride()
                            .withComplexFieldId("People.BirthInfo.BornAddress.Address")
                            .build()))
                    .build()
                )
                .build();

            CaseUpdateViewEvent eventTrigger = accessControlService.filterCaseViewFieldsByAccess(
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_CREATE);

            assertAll(
                () -> assertThat(eventTrigger.getCaseFields(), hasSize(2)),
                () -> assertThat(eventTrigger.getCaseFields(), not(hasItem(caseViewField1))),
                () -> assertThat(eventTrigger.getCaseFields(), hasItem(caseViewField2)),
                () -> assertThat(eventTrigger.getCaseFields().get(1).getId(), is("People")),
                () -> assertThat(eventTrigger.getCaseFields().get(1).getFieldTypeDefinition().getChildren().size(), is(4)),
                () -> assertThat(eventTrigger.getCaseFields().get(1).getFieldTypeDefinition().getChildren().get(0).getId(), is("LastName")),
                () -> assertThat(eventTrigger.getCaseFields().get(1).getFieldTypeDefinition().getChildren().get(3).getId(), is("Notes")),
                () -> assertThat(eventTrigger.getCaseFields().get(1).getFieldTypeDefinition()
                    .getChildren()
                    .get(1)
                    .getFieldTypeDefinition()
                    .getChildren().size(), is(2)),
                () -> assertThat(
                    eventTrigger.getCaseFields().get(1).getFieldTypeDefinition().getChildren().get(1).getFieldTypeDefinition().getChildren().get(0).getId(),
                    is("BornCity")
                ),

                () -> assertThat(
                    eventTrigger.getCaseFields().get(1).getFieldTypeDefinition().getChildren().get(1).getFieldTypeDefinition().getChildren().get(1).getId(),
                    is("BornAddress")
                ),
                () -> assertThat(
                    eventTrigger.getCaseFields()
                        .get(1)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(1)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(1)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .size(),
                    is(1)
                ),
                () -> assertThat(
                    eventTrigger.getCaseFields()
                        .get(1)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(1)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(1)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(0)
                        .getFieldTypeDefinition()
                        .getChildren().size(),
                    is(2)
                ),
                () -> assertThat(
                    eventTrigger.getCaseFields()
                        .get(1)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(1)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(1)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(0)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(0).getId(),
                    is("Line1")
                ),
                () -> assertThat(
                    eventTrigger.getCaseFields()
                        .get(1)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(1)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(1)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(0)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(1).getId(),
                    is("Country")
                ),

                () -> assertThat(eventTrigger
                    .getCaseFields()
                    .get(1)
                    .getFieldTypeDefinition()
                    .getChildren()
                    .get(2)
                    .getFieldTypeDefinition()
                    .getChildren().size(), is(1)),
                () -> assertThat(
                    eventTrigger.getCaseFields()
                        .get(1)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(2)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(0)
                        .getId(), is("Address")),
                () -> assertThat(
                    eventTrigger.getCaseFields()
                        .get(1)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(2)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(0)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .size(),
                    is(2)
                ),
                () -> assertThat(
                    eventTrigger.getCaseFields()
                        .get(1)
                        .getFieldTypeDefinition()
                        .getChildren().get(2)
                        .getFieldTypeDefinition()
                        .getChildren().get(0)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(0)
                        .getId(),
                    is("Line1")
                ),
                () -> assertThat(
                    eventTrigger.getCaseFields()
                        .get(1)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(2)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(0)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(1)
                        .getId(),
                    is("Line2")
                ),

                () -> assertThat(
                    eventTrigger.getCaseFields().get(1).getFieldTypeDefinition()
                        .getChildren().get(3).getFieldTypeDefinition()
                        .getChildren().get(0).getId(),
                    is("Tags")
                ),
                () -> assertThat(
                    eventTrigger.getCaseFields().get(1).getFieldTypeDefinition()
                        .getChildren()
                        .get(3)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(0)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .size(),
                    is(1)
                ),
                () -> assertThat(
                    eventTrigger.getCaseFields().get(1).getFieldTypeDefinition()
                        .getChildren().get(3)
                        .getFieldTypeDefinition()
                        .getChildren().get(0)
                        .getFieldTypeDefinition()
                        .getChildren()
                        .get(0).getId(),
                    is("Tag")
                ),

                () -> assertThat(eventTrigger.getWizardPages().get(0).getWizardPageFields(), hasSize(2)),
                () -> assertThat(eventTrigger.getWizardPages().get(0).getWizardPageFields().get(1).getComplexFieldOverrides(), hasSize(3)),
                () -> assertThat(
                    eventTrigger.getWizardPages().get(0).getWizardPageFields().get(1).getComplexFieldOverrides().get(0).getComplexFieldElementId(),
                    is("People.LastName")
                ),
                () -> assertThat(
                    eventTrigger.getWizardPages().get(0).getWizardPageFields().get(1).getComplexFieldOverrides().get(1).getComplexFieldElementId(),
                    is("People.BirthInfo.BornCity")
                ),
                () -> assertThat(
                    eventTrigger.getWizardPages().get(0).getWizardPageFields().get(1).getComplexFieldOverrides().get(2).getComplexFieldElementId(),
                    is("People.BirthInfo.BornAddress.Address")
                )
            );
        }

        @Test
        @DisplayName("Should filter all when filtered for UPDATE but ACLs are for CREATE")
        void filterAllByUpdateAccessWhenAllAccessIsOnCreate() {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
            people.setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .build()));
            people.setComplexACLs(asList(
                aComplexACL()
                    .withListElementCode("LastName")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("BirthInfo")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("BirthInfo.BornCity")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("BirthInfo.BornCountry")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(false)
                    .build(),
                aComplexACL()
                    .withListElementCode("BirthInfo.BornAddress")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("BirthInfo.BornAddress.Name")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(false)
                    .build(),
                aComplexACL()
                    .withListElementCode("BirthInfo.BornAddress.Address")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("BirthInfo.BornAddress.Address.Line1")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("BirthInfo.BornAddress.Address.Country")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Notes")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Notes.Txt")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(false)
                    .build(),
                aComplexACL()
                    .withListElementCode("Notes.Tags")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Notes.Tags.Tag")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Line1")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build(),
                aComplexACL()
                    .withListElementCode("Addresses.Address.Line2")
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .build()
            ));

            final CaseTypeDefinition caseType = newCaseType()
                .withField(people)
                .withField(newCaseField()
                    .withId("Name")
                    .withFieldType(aFieldType()
                        .withId("Text")
                        .withType("Text")
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(false)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("Surname")
                    .withFieldType(aFieldType()
                        .withId("Text")
                        .withType("Text")
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_2)
                        .withCreate(true)
                        .build())
                    .build())
                .build();
            caseType.getCaseFieldDefinitions().stream().forEach(caseField -> caseField.propagateACLsToNestedFields());

            final CaseViewField caseViewField1 = aViewField()
                .withId("Name")
                .build();
            final CaseViewField caseViewField2 = aViewField()
                .withId("Surname")
                .build();
            final CaseViewField caseViewField3 = aViewField()
                .withId("People")
                .withFieldType(aFieldType()
                    .withId("G339483948")
                    .withType(COLLECTION)
                    .build())
                .build();
            caseViewField3.getFieldTypeDefinition().setCollectionFieldTypeDefinition(getPersonFieldType());

            CaseUpdateViewEvent caseEventTrigger = newCaseUpdateViewEvent()
                .withField(caseViewField1)
                .withField(caseViewField2)
                .withField(caseViewField3)
                .withWizardPage(newWizardPage()
                    .withId("Page One")
                    .withField(caseViewField1)
                    .withField(caseViewField2)
                    .withField(caseViewField3)
                    .build()
                )
                .build();

            CaseUpdateViewEvent eventTrigger = accessControlService.filterCaseViewFieldsByAccess(
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_UPDATE);

            assertAll(
                () -> assertThat(eventTrigger.getCaseFields(), hasSize(0)),
                () -> assertThat(eventTrigger.getWizardPages().get(0).getWizardPageFields(), hasSize(0))
            );
        }

        @Test
        @DisplayName("Should leave Complex Field properly for predefined complex sub fields")
        void leaveComplexCaseFieldWithPredefinedChildrenByUpdateAccess() {
            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Name")
                    .withFieldType(aFieldType()
                        .withId("Text")
                        .withType("Text")
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(false)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("Surname")
                    .withFieldType(aFieldType()
                        .withId("Text")
                        .withType("Text")
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_2)
                        .withCreate(true)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("Address")
                    .withFieldType(aFieldType()
                        .withId(PREDEFINED_COMPLEX_ADDRESS_UK)
                        .withType("Complex")
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_2)
                        .withCreate(true)
                        .build())
                    .build())
                .build();
            caseType.getCaseFieldDefinitions().get(2).getFieldTypeDefinition().setComplexFields(getPredefinedAddressFields());
            caseType.getCaseFieldDefinitions().stream().forEach(caseField -> caseField.propagateACLsToNestedFields());

            final CaseViewField caseViewField1 = aViewField()
                .withId("Name")
                .build();
            final CaseViewField caseViewField2 = aViewField()
                .withId("Surname")
                .build();
            final CaseViewField caseViewField3 = aViewField()
                .withId("Address")
                .withFieldType(aFieldType()
                    .withId(PREDEFINED_COMPLEX_ADDRESS_UK)
                    .withType("Complex")
                    .build())
                .build();
            caseViewField3.getFieldTypeDefinition().setComplexFields(getPredefinedAddressFields());

            CaseUpdateViewEvent caseEventTrigger = newCaseUpdateViewEvent()
                .withField(caseViewField1)
                .withField(caseViewField2)
                .withField(caseViewField3)
                .withWizardPage(newWizardPage()
                    .withId("Page One")
                    .withField(caseViewField1)
                    .withField(caseViewField2)
                    .withField(caseViewField3)
                    .build()
                )
                .build();

            CaseUpdateViewEvent eventTrigger = accessControlService.filterCaseViewFieldsByAccess(
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_CREATE);

            assertAll(
                () -> assertThat(eventTrigger.getCaseFields(), hasSize(2)),
                () -> assertThat(eventTrigger.getWizardPages().get(0).getWizardPageFields(), hasSize(2))
            );
        }

        @Test
        @DisplayName("Should filter Complex Field properly for predefined complex sub fields with no access")
        void filterComplexCaseFieldChildrenByUpdateAccessWhenAllAccessIsOnCreate() {
            final CaseTypeDefinition caseType = newCaseType()
                .withField(newCaseField()
                    .withId("Name")
                    .withFieldType(aFieldType()
                        .withId("Text")
                        .withType("Text")
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES)
                        .withCreate(false)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("Surname")
                    .withFieldType(aFieldType()
                        .withId("Text")
                        .withType("Text")
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_2)
                        .withCreate(true)
                        .build())
                    .build())
                .withField(newCaseField()
                    .withId("Address")
                    .withFieldType(aFieldType()
                        .withId(PREDEFINED_COMPLEX_ADDRESS_UK)
                        .withType("Complex")
                        .build())
                    .withAcl(anAcl()
                        .withRole(ROLE_IN_USER_ROLES_2)
                        .withCreate(false)
                        .build())
                    .build())
                .build();
            caseType.getCaseFieldDefinitions().get(2).getFieldTypeDefinition().setComplexFields(getPredefinedAddressFields());
            caseType.getCaseFieldDefinitions().stream().forEach(caseField -> caseField.propagateACLsToNestedFields());

            final CaseViewField caseViewField1 = aViewField()
                .withId("Name")
                .build();
            final CaseViewField caseViewField2 = aViewField()
                .withId("Surname")
                .build();
            final CaseViewField caseViewField3 = aViewField()
                .withId("Address")
                .withFieldType(aFieldType()
                    .withId(PREDEFINED_COMPLEX_ADDRESS_UK)
                    .withType("Complex")
                    .build())
                .build();
            caseViewField3.getFieldTypeDefinition().setComplexFields(getPredefinedAddressFields());

            CaseUpdateViewEvent caseEventTrigger = newCaseUpdateViewEvent()
                .withField(caseViewField1)
                .withField(caseViewField2)
                .withField(caseViewField3)
                .withWizardPage(newWizardPage()
                    .withId("Page One")
                    .withField(caseViewField1)
                    .withField(caseViewField2)
                    .withField(caseViewField3)
                    .build()
                )
                .build();

            CaseUpdateViewEvent eventTrigger = accessControlService.filterCaseViewFieldsByAccess(
                caseEventTrigger,
                caseType.getCaseFieldDefinitions(),
                USER_ROLES,
                CAN_CREATE);

            assertAll(
                () -> assertThat(eventTrigger.getCaseFields(), hasSize(1)),
                () -> assertThat(eventTrigger.getWizardPages().get(0).getWizardPageFields(), hasSize(1)),
                () -> assertThat(eventTrigger.getCaseFields(), hasItem(caseViewField2))
            );
        }
    }

    @Nested
    @DisplayName("updateCollectionDisplayContextParameter for Event Triggers Tests")
    class UpdateCollectionDisplayContextParameterByAccessTests {

        @Test
        @DisplayName("Should set #COLLECTION(allowInsert,allowDelete) in DisplayContextParameter of a collection "
            + "caseField when create and delete ACLs are set")
        void updateCollectionDisplayContextParameterWhenFieldHasCreateDeleteRoles() {

            final CaseViewField caseViewField1 = aViewField()
                .withId("People")
                .withFieldType(aFieldType()
                    .withId("G339483948")
                    .withType(COLLECTION)
                    .build())
                .withACL(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(true)
                    .withDelete(true)
                    .withUpdate(false)
                    .build())
                .build();
            caseViewField1.getFieldTypeDefinition().setCollectionFieldTypeDefinition(getPersonFieldType());
            caseViewField1.getFieldTypeDefinition().getChildren().stream()
                .filter(e -> e.getId().equals("Addresses")).findFirst()
                .get().setAccessControlLists(asList(anAcl()
                .withRole(ROLE_IN_USER_ROLES)
                .withCreate(true)
                .withDelete(true)
                .withUpdate(false)
                .build()));

            CaseUpdateViewEvent caseEventTrigger = newCaseUpdateViewEvent()
                .withField(caseViewField1)
                .withWizardPage(newWizardPage()
                        .withId("Page One")
                        .withField(caseViewField1)
                        .build()
                               )
                .build();

            CaseUpdateViewEvent eventTrigger = accessControlService.updateCollectionDisplayContextParameterByAccess(
                caseEventTrigger,
                USER_ROLES);

            assertThat("There should be only one caseField", eventTrigger.getCaseFields(), hasSize(1));

            CaseViewField people = eventTrigger.getCaseFields().stream()
                .filter(e -> e.getId().equals("People")).findFirst().get();

            assertAll(
                () -> assertNotNull(people),
                () -> assertTrue(people.getDisplayContextParameter().contains("#COLLECTION(")),
                () -> assertTrue(people.getDisplayContextParameter().contains("allowInsert")),
                () -> assertTrue(people.getDisplayContextParameter().contains("allowDelete"))
            );

            CaseFieldDefinition addresses = people.getFieldTypeDefinition().getChildren().stream()
                .filter(e -> e.getId().equals("Addresses")).findFirst()
                .get();

            assertAll(
                () -> assertTrue(addresses.getDisplayContextParameter().contains("#COLLECTION(")),
                () -> assertTrue(addresses.getDisplayContextParameter().contains("allowInsert")),
                () -> assertTrue(addresses.getDisplayContextParameter().contains("allowDelete"))
            );
        }

        @Test
        @DisplayName("Should set #COLLECTION(allowInsert,allowDelete) in DisplayContextParameter of a collection "
            + "caseField when an update ACL is set")
        void updateCollectionDisplayContextParameterWhenFieldHasUpdateRole() {

            final CaseViewField caseViewField1 = aViewField()
                .withId("People")
                .withFieldType(aFieldType()
                    .withId("G339483948")
                    .withType(COLLECTION)
                    .build())
                .withACL(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(false)
                    .withDelete(false)
                    .withUpdate(true)
                    .build())
                .build();
            caseViewField1.getFieldTypeDefinition().setCollectionFieldTypeDefinition(getPersonFieldType());

            CaseUpdateViewEvent caseEventTrigger = newCaseUpdateViewEvent()
                .withField(caseViewField1)
                .withWizardPage(newWizardPage()
                        .withId("Page One")
                        .withField(caseViewField1)
                        .build()
                               )
                .build();

            CaseUpdateViewEvent eventTrigger = accessControlService.updateCollectionDisplayContextParameterByAccess(
                caseEventTrigger,
                USER_ROLES);

            CaseViewField people = eventTrigger.getCaseFields().stream()
                .filter(e -> e.getId().equals("People")).findFirst().get();

            assertAll(
                () -> assertNotNull(people),
                () -> assertTrue(people.getDisplayContextParameter().contains("#COLLECTION(")),
                () -> assertTrue(people.getDisplayContextParameter().contains("allowInsert")),
                () -> assertTrue(people.getDisplayContextParameter().contains("allowDelete"))
            );
        }

        @Test
        @DisplayName("Should set #COLLECTION() in DisplayContextParameter of a collection caseField when no ACL set")
        void updateCollectionDisplayContextParameterWhenFieldHasNoCreateDeleteUpdateRoles() {

            final CaseViewField caseViewField1 = aViewField()
                .withId("People")
                .withFieldType(aFieldType()
                    .withId("G339483948")
                    .withType(COLLECTION)
                    .build())
                .withACL(anAcl()
                    .withRole(ROLE_IN_USER_ROLES)
                    .withCreate(false)
                    .withDelete(false)
                    .withUpdate(false)
                    .build())
                .build();
            caseViewField1.getFieldTypeDefinition().setCollectionFieldTypeDefinition(getPersonFieldType());

            CaseUpdateViewEvent caseEventTrigger = newCaseUpdateViewEvent()
                .withField(caseViewField1)
                .withWizardPage(newWizardPage()
                        .withId("Page One")
                        .withField(caseViewField1)
                        .build()
                               )
                .build();

            CaseUpdateViewEvent eventTrigger = accessControlService.updateCollectionDisplayContextParameterByAccess(
                caseEventTrigger,
                USER_ROLES);

            CaseViewField people = eventTrigger.getCaseFields().stream()
                .filter(e -> e.getId().equals("People")).findFirst().get();

            assertAll(
                () -> assertNotNull(people),
                () -> assertTrue(people.getDisplayContextParameter().contains("#COLLECTION(")),
                () -> assertFalse(people.getDisplayContextParameter().contains("allowInsert")),
                () -> assertFalse(people.getDisplayContextParameter().contains("allowDelete"))
            );
        }
    }

    @Nested
    @DisplayName("FilterCaseFieldsByAccess Tests - Simple CaseFields")
    class FilterCaseFieldsByAccessSimpleFieldTests {
        @Test
        @DisplayName("Should not filter and case field if user has all required ACLs")
        void doNotFilterCaseFieldsIfUserHasAccess() {
            final CaseFieldDefinition caseField1 = newCaseField()
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
            final CaseFieldDefinition caseField2 = newCaseField()
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
            final CaseFieldDefinition caseField3 = newCaseField()
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
            List<CaseFieldDefinition> caseFieldDefinitions = Arrays.asList(caseField1, caseField2, caseField3);

            final List<CaseFieldDefinition> filteredCaseFields = accessControlService.filterCaseFieldsByAccess(caseFieldDefinitions,
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
            final CaseFieldDefinition caseField1 = newCaseField()
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
            final CaseFieldDefinition caseField2 = newCaseField()
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
            final CaseFieldDefinition caseField3 = newCaseField()
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
            List<CaseFieldDefinition> caseFieldDefinitions = Arrays.asList(caseField1, caseField2, caseField3);

            final List<CaseFieldDefinition> filteredCaseFields = accessControlService.filterCaseFieldsByAccess(caseFieldDefinitions,
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
    @DisplayName("FilterCaseFieldsByAccess Tests - Compound CaseFields")
    class FilterCaseFieldsByAccessCompoundFieldTests {
        @Test
        @DisplayName("Should filter sub fields of caseFields based on Complex ACLs on READ")
        void filterCaseFieldsUserHasReadAccess() {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
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

            final List<CaseFieldDefinition> filteredCaseFields = accessControlService.filterCaseFieldsByAccess(asList(people), USER_ROLES, CAN_READ);

            assertAll(
                () -> assertThat(filteredCaseFields, hasSize(1)),
                () -> assertThat(filteredCaseFields.get(0).getFieldTypeDefinition().getChildren(), hasSize(3)),
                () -> assertThat(filteredCaseFields.get(0).getFieldTypeDefinition().getChildren().get(2).getId(), is("Addresses")),
                () -> assertThat(filteredCaseFields.get(0).getFieldTypeDefinition().getChildren().get(2).getFieldTypeDefinition().getChildren().size(), is(1)),
                () -> assertThat(
                    filteredCaseFields.get(0).getFieldTypeDefinition().getChildren()
                        .get(2).getFieldTypeDefinition()
                        .getChildren().get(0)
                        .getFieldTypeDefinition()
                        .getChildren().size(),
                    is(1)
                )
            );
        }

        @Test
        @DisplayName("Should filter sub fields of caseFields based on Complex ACLs on UPDATE")
        void filterCaseFieldsUserHasUpdateAccess() {
            final CaseFieldDefinition people = getPeopleCollectionFieldDefinition();
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

            final List<CaseFieldDefinition> filteredCaseFields = accessControlService.filterCaseFieldsByAccess(asList(people), USER_ROLES, CAN_UPDATE);

            assertAll(
                () -> assertThat(filteredCaseFields, hasSize(1)),
                () -> assertThat(filteredCaseFields.get(0).getFieldTypeDefinition().getChildren(), hasSize(3)),
                () -> assertThat(filteredCaseFields.get(0).getFieldTypeDefinition().getChildren().get(2).getId(), is("Addresses")),
                () -> assertThat(filteredCaseFields.get(0).getFieldTypeDefinition().getChildren().get(2).getFieldTypeDefinition().getChildren().size(), is(1)),
                () -> assertThat(
                    filteredCaseFields.get(0).getFieldTypeDefinition()
                        .getChildren().get(2).getFieldTypeDefinition()
                        .getChildren().get(0).getFieldTypeDefinition()
                        .getChildren().size(),
                    is(1)
                )
            );
        }
    }

    static List<CaseFieldDefinition> getPredefinedAddressFields() {
        return asList(
            newCaseField()
                .withId("AddressLine1")
                .withFieldType(aFieldType()
                    .withId("Text")
                    .withType("Text")
                    .build())
                .build(),
            newCaseField()
                .withId("AddressLine2")
                .withFieldType(aFieldType()
                    .withId("Text")
                    .withType("Text")
                    .build())
                .build(),
            newCaseField()
                .withId("AddressLine3")
                .withFieldType(aFieldType()
                    .withId("Text")
                    .withType("Text")
                    .build())
                .build(),
            newCaseField()
                .withId("PostCode")
                .withFieldType(aFieldType()
                    .withId("Text")
                    .withType("Text")
                    .build())
                .build(),
            newCaseField()
                .withId("PostTown")
                .withFieldType(aFieldType()
                    .withId("Text")
                    .withType("Text")
                    .build())
                .build(),
            newCaseField()
                .withId("County")
                .withFieldType(aFieldType()
                    .withId("Text")
                    .withType("Text")
                    .build())
                .build(),
            newCaseField()
                .withId("Country")
                .withFieldType(aFieldType()
                    .withId("Text")
                    .withType("Text")
                    .build())
                .build()
        );
    }
}
