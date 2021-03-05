package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.caseaccess.CaseRoleRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.ComplexACL;
import uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole.CREATOR;
import static uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.RoleToAccessProfilesMappingsGenerator.IDAM_PREFIX;

@DisplayName("RoleToAccessProfilesMappingsGeneratorTest")
class RoleToAccessProfilesMappingsGeneratorTest {

    private static final String CASE_TYPE_ID = "testCaseId";
    private static final Set<String> SET_WITH_CREATOR_CASE_ROLE = Collections.singleton(CREATOR.getRole());
    private static final Set<String> SET_WITHOUT_CREATOR_CASE_ROLE = Collections.singleton("[Claimant]");
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

    CaseTypeDefinition caseTypeDefinition;

    @Mock
    private CaseRoleRepository caseRoleRepository;

    @InjectMocks
    private RoleToAccessProfilesMappingsGenerator instance;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        instance = new RoleToAccessProfilesMappingsGenerator(caseRoleRepository);

        caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setId(CASE_TYPE_ID);

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
            given(caseRoleRepository.getCaseRoles(CASE_TYPE_ID)).willReturn(SET_WITH_CREATOR_CASE_ROLE);

            List<AccessProfile> generated = instance.generate(caseTypeDefinition);

            assertAll(
                () -> assertThat("Expected number of access profiles to be 11", generated, hasSize(11)),
                () -> assertTrue(findAccessProfile(generated, withIdamPrefix(IDAM_ROLE1), IDAM_ROLE1).isPresent()),
                () -> assertTrue(findAccessProfile(generated, withIdamPrefix(IDAM_ROLE2), IDAM_ROLE2).isPresent()),
                () -> assertTrue(findAccessProfile(generated, withIdamPrefix(IDAM_ROLE3), IDAM_ROLE3).isPresent()),
                () -> assertTrue(findAccessProfile(generated, withIdamPrefix(IDAM_ROLE4), IDAM_ROLE4).isPresent()),
                () -> assertTrue(findAccessProfile(generated, withIdamPrefix(IDAM_ROLE5), IDAM_ROLE5).isPresent()),
                () -> assertTrue(findAccessProfile(generated, withIdamPrefix(IDAM_ROLE6), IDAM_ROLE6).isPresent()),
                () -> assertTrue(findAccessProfile(generated, CASE_ROLE1, CASE_ROLE1).isPresent()),
                () -> assertTrue(findAccessProfile(generated, CASE_ROLE2, CASE_ROLE2).isPresent()),
                () -> assertTrue(findAccessProfile(generated, CASE_ROLE3, CASE_ROLE3).isPresent()),
                () -> assertTrue(findAccessProfile(generated, CASE_ROLE4, CASE_ROLE4).isPresent()),
                () -> assertTrue(findAccessProfile(generated, CASE_ROLE5, CASE_ROLE5).isPresent())
            );
        }

        @Test
        public void shouldGenerateAccessProfilesMappingsWhenNoCreatorCaseRoleDefined() {
            given(caseRoleRepository.getCaseRoles(CASE_TYPE_ID)).willReturn(SET_WITHOUT_CREATOR_CASE_ROLE);

            List<AccessProfile> generated = instance.generate(caseTypeDefinition);

            Optional<AccessProfile> creatorAccessProfile = generated.stream()
                .filter(ap -> ap.getRoleName().equals(CREATOR.getRole()))
                .filter(ap -> ap.getRoleName().equals(CREATOR.getRole()))
                .findAny();

            assertAll(
                () -> assertThat("Expected number of access profiles to be 12", generated, hasSize(12)),
                () -> assertTrue(creatorAccessProfile.isPresent()),
                () -> assertThat("Expected to match the caseTypeId",
                                 creatorAccessProfile.get().getCaseTypeId(), is(CASE_TYPE_ID))
            );
        }
    }

    private Optional<AccessProfile> findAccessProfile(List<AccessProfile> generated,
                                                      String roleName, String accessProfileName) {
        return generated.stream()
            .filter(ap -> ap.getRoleName().equals(roleName))
            .filter(ap -> ap.getAccessProfiles().contains(accessProfileName))
            .findAny();
    }

    private String withIdamPrefix(String idamRole) {
        return IDAM_PREFIX + idamRole;
    }

    private void addCaseTypeAcls(CaseTypeDefinition caseTypeDefinition) {
        caseTypeDefinition.setAccessControlLists(asList(aclWithRole(IDAM_ROLE1),
                                                        aclWithRole(IDAM_ROLE2),
                                                        aclWithRole(CASE_ROLE1)));
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
