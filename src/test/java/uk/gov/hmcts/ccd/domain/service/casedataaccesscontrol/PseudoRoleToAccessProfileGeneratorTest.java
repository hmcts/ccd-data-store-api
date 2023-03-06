package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.ComplexACL;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;
import uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole.CREATOR;

@DisplayName("PseudoRoleToAccessProfilesGeneratorTest")
class PseudoRoleToAccessProfileGeneratorTest {

    public static final String IDAM_ROLE1 = "caseworker-caa-caseType";
    public static final String IDAM_ROLE2 = "caseworker-approver";
    public static final String IDAM_ROLE3 = "citizen";
    public static final String IDAM_ROLE4 = "caseworker-approver-event";
    public static final String IDAM_ROLE5 = "caseworker-caseField";
    public static final String IDAM_ROLE6 = "caseworker-complex-caseField";
    public static final String CASE_ROLE1 = "[Claimant]";
    public static final String CASE_ROLE2 = "[DefendantState]";
    public static final String CASE_ROLE3 = "[DefendantEvent]";
    public static final String CASE_ROLE4 = "[ClaimantCaseField]";
    public static final String CASE_ROLE5 = "[ClaimantComplexCaseField]";
    public static final String CASE_ROLE6 = "[Respondent12-_]";

    CaseTypeDefinition caseTypeDefinition;

    @InjectMocks
    private PseudoRoleToAccessProfileGenerator instance;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        caseTypeDefinition = new CaseTypeDefinition();

        addCaseTypeAcls(caseTypeDefinition);
        addStateAcls(caseTypeDefinition);
        addEventAcls(caseTypeDefinition);
        addCaseFieldAcls(caseTypeDefinition);
    }

    @Nested
    @DisplayName("generate()")
    class Generate {

        @Test
        public void shouldGenerateAccessProfilesMappings() {

            List<RoleToAccessProfileDefinition> generated = instance.generate(caseTypeDefinition);

            assertAll(
                () -> assertThat("Invalid expected number of access profiles", generated, hasSize(13)),
                () -> assertTrue(findAccessProfile(generated, IDAM_ROLE1).isPresent()),
                () -> assertTrue(findAccessProfile(generated, IDAM_ROLE2).isPresent()),
                () -> assertTrue(findAccessProfile(generated, IDAM_ROLE3).isPresent()),
                () -> assertTrue(findAccessProfile(generated, IDAM_ROLE4).isPresent()),
                () -> assertTrue(findAccessProfile(generated, IDAM_ROLE5).isPresent()),
                () -> assertTrue(findAccessProfile(generated, IDAM_ROLE6).isPresent()),
                () -> assertTrue(findAccessProfile(generated, CASE_ROLE1).isPresent()),
                () -> assertTrue(findAccessProfile(generated, CASE_ROLE2).isPresent()),
                () -> assertTrue(findAccessProfile(generated, CASE_ROLE3).isPresent()),
                () -> assertTrue(findAccessProfile(generated, CASE_ROLE4).isPresent()),
                () -> assertTrue(findAccessProfile(generated, CASE_ROLE5).isPresent()),
                () -> assertTrue(findAccessProfile(generated, CASE_ROLE6).isPresent()),
                () -> assertTrue(findAccessProfile(generated, CREATOR.getRole()).isPresent())
            );
        }
    }

    private Optional<RoleToAccessProfileDefinition> findAccessProfile(List<RoleToAccessProfileDefinition> generated,
                                                                      String accessProfileName) {
        return generated.stream()
            .filter(ap -> ap.getAccessProfiles().equals(accessProfileName))
            .findAny();
    }

    private void addCaseTypeAcls(CaseTypeDefinition caseTypeDefinition) {
        caseTypeDefinition.setAccessControlLists(asList(aclWithRole(IDAM_ROLE1),
                                                        aclWithRole(IDAM_ROLE2),
                                                        aclWithRole(CASE_ROLE1),
                                                        aclWithRole(CASE_ROLE6)));
    }

    private void addStateAcls(CaseTypeDefinition caseTypeDefinition) {
        caseTypeDefinition.setStates(asList(
            caseStateWithAcl(asList(aclWithRole(IDAM_ROLE3),
                                    aclWithRole(CASE_ROLE1))),
            caseStateWithAcl(List.of(aclWithRole(CASE_ROLE2)))
        ));
    }

    private CaseStateDefinition caseStateWithAcl(List<AccessControlList> acls) {
        CaseStateDefinition state = new CaseStateDefinition();
        state.setAccessControlLists(acls);
        return state;
    }

    private void addEventAcls(CaseTypeDefinition caseTypeDefinition) {
        caseTypeDefinition.setEvents(asList(createEventWithAcl(asList(aclWithRole(IDAM_ROLE4),
                                                                      aclWithRole(CASE_ROLE3))),
                                            createEventWithAcl(asList(aclWithRole(IDAM_ROLE3),
                                                                      aclWithRole(CASE_ROLE1)))));
    }

    private CaseEventDefinition createEventWithAcl(List<AccessControlList> acls) {
        CaseEventDefinition event = new CaseEventDefinition();
        event.setAccessControlLists(acls);
        return event;
    }

    private void addCaseFieldAcls(CaseTypeDefinition caseTypeDefinition) {
        List<AccessControlList> acls = asList(aclWithRole(IDAM_ROLE5),
                                              aclWithRole(CASE_ROLE4));
        List<ComplexACL> complexAcls = asList(complexAclWithRole(IDAM_ROLE6),
                                              complexAclWithRole(CASE_ROLE5));
        caseTypeDefinition.setCaseFieldDefinitions(List.of(createCaseField(acls, complexAcls)));
    }

    private CaseFieldDefinition createCaseField(List<AccessControlList> acls, List<ComplexACL> complexAcls) {
        CaseFieldDefinition caseField = new CaseFieldDefinition();
        caseField.setAccessControlLists(acls);

        caseField.setComplexACLs(complexAcls);
        return caseField;
    }

    private ComplexACL complexAclWithRole(String role) {
        return TestBuildersUtil.ComplexACLBuilder.aComplexACL().withRole(role).build();
    }

    private AccessControlList aclWithRole(String role) {
        return TestBuildersUtil.AccessControlListBuilder.anAcl().withRole(role).build();
    }
}
