package uk.gov.hmcts.ccd.data.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentAttributes;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.RoleAssignmentService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

class AttributeBasedJurisdictionsResolverTest {

    private static final String USER_ID = "12345";
    private static final String DIVORCE_SOLICITOR_ROLE = "caseworker-divorce-solicitor";
    private static final String CASEWORKER_CMC_ROLE = "caseworker-cmc";
    private static final String PROBATE_SOLICITOR_ROLE = "caseworker-probate-solicitor";
    private static final String DIVORCE_JURISDICTION = "DIVORCE";
    private static final String PROBATE_JURISDICTION = "PROBATE";
    private static final String CASE_ID = "123456789";

    @Mock
    private IdamUser idamUser;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private RoleAssignmentService roleAssignmentService;

    private JurisdictionsResolver jurisdictionsResolver;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        jurisdictionsResolver = new AttributeBasedJurisdictionsResolver(userRepository,
            roleAssignmentService,
            caseDefinitionRepository);
    }

    @Test
    void shouldReturnNoJurisdictionsForEmptyRoleAssignments() {
        given(idamUser.getId()).willReturn(USER_ID);
        given(userRepository.getUser()).willReturn(idamUser);

        given(roleAssignmentService.getRoleAssignments(USER_ID)).willReturn(
            RoleAssignments.builder()
                .roleAssignments(Collections.emptyList())
            .build()
        );

        assertTrue(jurisdictionsResolver.getJurisdictions().isEmpty());

        verify(roleAssignmentService).getRoleAssignments(USER_ID);
    }

    @Test
    void shouldReturnJurisdictionsForValidRoleAssignments() {
        given(idamUser.getId()).willReturn(USER_ID);
        given(userRepository.getUser()).willReturn(idamUser);

        given(roleAssignmentService.getRoleAssignments(USER_ID)).willReturn(
            RoleAssignments.builder()
                .roleAssignments(asList(
                    RoleAssignment.builder()
                        .grantType("CASE")
                        .roleName(DIVORCE_SOLICITOR_ROLE)
                        .actorId(USER_ID)
                        .attributes(RoleAssignmentAttributes.builder()
                            .jurisdiction(Optional.of(DIVORCE_JURISDICTION)).build()).build(),
                    RoleAssignment.builder()
                        .grantType("CASE")
                        .roleName(DIVORCE_SOLICITOR_ROLE)
                        .actorId(USER_ID)
                        .attributes(RoleAssignmentAttributes.builder().build()).build(),
                    RoleAssignment.builder()
                        .grantType("CASE")
                        .roleName(CASEWORKER_CMC_ROLE)
                        .actorId(USER_ID)
                        .attributes(RoleAssignmentAttributes.builder()
                            .caseId(Optional.of(CASE_ID)).build()).build(),
                    RoleAssignment.builder()
                        .grantType("CASE")
                        .roleName(PROBATE_SOLICITOR_ROLE)
                        .actorId(USER_ID)
                        .attributes(RoleAssignmentAttributes.builder()
                            .jurisdiction(Optional.of(PROBATE_JURISDICTION)).build()).build()
                ))
            .build()
        );

        List<String> result = jurisdictionsResolver.getJurisdictions();
        assertThat(result.size(), is(2));
        assertTrue(result.contains(DIVORCE_JURISDICTION));
        assertTrue(result.contains(PROBATE_JURISDICTION));
    }

    @Test
    void shouldReturnJurisdictionsOnlyWhereGrantIsNotBasic() {
        given(idamUser.getId()).willReturn(USER_ID);
        given(userRepository.getUser()).willReturn(idamUser);

        given(roleAssignmentService.getRoleAssignments(USER_ID)).willReturn(
            RoleAssignments.builder()
                .roleAssignments(asList(
                    RoleAssignment.builder()
                        .grantType(GrantType.BASIC.name())
                        .roleName(DIVORCE_SOLICITOR_ROLE)
                        .actorId(USER_ID)
                        .attributes(RoleAssignmentAttributes.builder()
                            .jurisdiction(Optional.of(DIVORCE_JURISDICTION)).build()).build(),
                    RoleAssignment.builder()
                        .grantType(GrantType.CHALLENGED.name())
                        .roleName(PROBATE_SOLICITOR_ROLE)
                        .actorId(USER_ID)
                        .attributes(RoleAssignmentAttributes.builder()
                            .jurisdiction(Optional.of(PROBATE_JURISDICTION)).build()).build()
                ))
                .build()
        );
        List<String> result = jurisdictionsResolver.getJurisdictions();
        assertThat(result.size(), is(1));
        assertTrue(result.contains(PROBATE_JURISDICTION));
    }

    @Test
    void shouldReturnJurisdictionsWhereJurisdictionProvided() {
        given(idamUser.getId()).willReturn(USER_ID);
        given(userRepository.getUser()).willReturn(idamUser);

        given(roleAssignmentService.getRoleAssignments(USER_ID)).willReturn(
            RoleAssignments.builder()
                .roleAssignments(asList(
                    RoleAssignment.builder()
                        .grantType(GrantType.CHALLENGED.name())
                        .roleName(PROBATE_SOLICITOR_ROLE)
                        .actorId(USER_ID)
                        .attributes(RoleAssignmentAttributes.builder()
                            .jurisdiction(Optional.of(PROBATE_JURISDICTION)).build()).build()
                ))
                .build()
        );

        List<String> result = jurisdictionsResolver.getJurisdictions();
        assertThat(result.size(), is(1));
        assertTrue(result.contains(PROBATE_JURISDICTION));
    }

    @Test
    void shouldReturnJurisdictionsWhereNoCaseTypeOrJurisdictionProvided() {
        given(idamUser.getId()).willReturn(USER_ID);
        given(userRepository.getUser()).willReturn(idamUser);
        final JurisdictionDefinition jurisdictionDefinition = new JurisdictionDefinition();
        jurisdictionDefinition.setId("jurisdictionId");
        final JurisdictionDefinition jurisdictionDefinition1 = new JurisdictionDefinition();
        jurisdictionDefinition1.setId(PROBATE_JURISDICTION);
        given(caseDefinitionRepository
            .getAllJurisdictionsFromDefinitionStore())
            .willReturn(List.of(jurisdictionDefinition, jurisdictionDefinition1));

        given(roleAssignmentService.getRoleAssignments(USER_ID)).willReturn(
            RoleAssignments.builder()
                .roleAssignments(asList(
                    RoleAssignment.builder()
                        .grantType(GrantType.STANDARD.name())
                        .roleName(DIVORCE_SOLICITOR_ROLE)
                        .actorId(USER_ID)
                        .roleType("ORGANISATION")
                        .attributes(RoleAssignmentAttributes.builder().build()).build(),
                    RoleAssignment.builder()
                        .grantType(GrantType.STANDARD.name())
                        .roleName(PROBATE_SOLICITOR_ROLE)
                        .actorId(USER_ID)
                        .attributes(RoleAssignmentAttributes.builder()
                            .jurisdiction(Optional.of(PROBATE_JURISDICTION)).build()).build()
                ))
                .build()
        );

        List<String> result = jurisdictionsResolver.getJurisdictions();
        assertThat(result.size(), is(2));
        assertTrue(result.contains(PROBATE_JURISDICTION));
        assertTrue(result.contains("jurisdictionId"));
    }

    @Test
    void shouldReturnJurisdictionsWhereCaseTypeProvided() {
        given(idamUser.getId()).willReturn(USER_ID);
        given(userRepository.getUser()).willReturn(idamUser);
        final JurisdictionDefinition jurisdictionDefinition = new JurisdictionDefinition();
        jurisdictionDefinition.setId("jurisdictionId");

        final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setJurisdictionDefinition(jurisdictionDefinition);
        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType("caseTypeId");

        given(roleAssignmentService.getRoleAssignments(USER_ID)).willReturn(
            RoleAssignments.builder()
                .roleAssignments(asList(
                    RoleAssignment.builder()
                        .grantType(GrantType.CHALLENGED.name())
                        .roleName(DIVORCE_SOLICITOR_ROLE)
                        .actorId(USER_ID)
                        .attributes(RoleAssignmentAttributes.builder()
                            .caseType(Optional.of("caseTypeId")).build()).build(),
                    RoleAssignment.builder()
                        .grantType(GrantType.CHALLENGED.name())
                        .roleName(PROBATE_SOLICITOR_ROLE)
                        .actorId(USER_ID)
                        .attributes(RoleAssignmentAttributes.builder()
                            .jurisdiction(Optional.of(PROBATE_JURISDICTION)).build()).build()
                ))
                .build()
        );

        List<String> result = jurisdictionsResolver.getJurisdictions();
        assertThat(result.size(), is(2));
        assertTrue(result.contains(PROBATE_JURISDICTION));
        assertTrue(result.contains("jurisdictionId"));
    }
}
