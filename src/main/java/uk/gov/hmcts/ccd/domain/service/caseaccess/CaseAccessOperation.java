package uk.gov.hmcts.ccd.domain.service.caseaccess;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.util.set.Sets;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseRoleRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseRoleRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserEntity;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.supplementarydata.SupplementaryDataRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentsDeleteRequest;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRoleWithOrganisation;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.RoleAssignmentService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.InvalidCaseRoleException;
import uk.gov.hmcts.ccd.v2.external.domain.CaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole.CREATOR;

@Service
public class CaseAccessOperation {

    public static final String ORGS_ASSIGNED_USERS_PATH = "orgs_assigned_users.";

    private final CaseUserRepository caseUserRepository;
    private final CaseDetailsRepository caseDetailsRepository;
    private final CaseRoleRepository caseRoleRepository;
    private final SupplementaryDataRepository supplementaryDataRepository;
    private final RoleAssignmentService roleAssignmentService;
    private final ApplicationParams applicationParams;

    public CaseAccessOperation(final @Qualifier(CachedCaseUserRepository.QUALIFIER)
                                   CaseUserRepository caseUserRepository,
                               @Qualifier(CachedCaseDetailsRepository.QUALIFIER)
                               final CaseDetailsRepository caseDetailsRepository,
                               @Qualifier(CachedCaseRoleRepository.QUALIFIER) CaseRoleRepository caseRoleRepository,
                               @Qualifier("default") SupplementaryDataRepository supplementaryDataRepository,
                               RoleAssignmentService roleAssignmentService,
                               ApplicationParams applicationParams) {

        this.caseUserRepository = caseUserRepository;
        this.caseDetailsRepository = caseDetailsRepository;
        this.caseRoleRepository = caseRoleRepository;
        this.supplementaryDataRepository = supplementaryDataRepository;
        this.roleAssignmentService = roleAssignmentService;
        this.applicationParams = applicationParams;
    }

    @Transactional
    public void grantAccess(final String jurisdictionId, final String caseReference, final String userId) {
        final Optional<CaseDetails> maybeCase = caseDetailsRepository.findByReference(jurisdictionId,
            Long.valueOf(caseReference));

        final var caseDetails = maybeCase.orElseThrow(() -> new CaseNotFoundException(caseReference));

        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            var currentRoles
                = roleAssignmentService.findRoleAssignmentsByCasesAndUsers(List.of(caseReference), List.of(userId));

            // if user does not have CREATOR role for case
            if (currentRoles.stream().noneMatch(cauRole -> cauRole.getCaseRole().equals(CREATOR.getRole()))) {
                roleAssignmentService.createCaseRoleAssignments(caseDetails, userId, Set.of(CREATOR.getRole()), false);
            }
        }
        if (applicationParams.getEnableCaseUsersDbSync()) {
            caseUserRepository.grantAccess(Long.valueOf(caseDetails.getId()), userId, CREATOR.getRole());
        }
    }

    @Transactional
    public void revokeAccess(final String jurisdictionId, final String caseReference, final String userId) {
        final Optional<CaseDetails> maybeCase = caseDetailsRepository.findByReference(jurisdictionId,
            Long.valueOf(caseReference));
        final var caseDetails = maybeCase.orElseThrow(() -> new CaseNotFoundException(caseReference));

        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            var deleteRequest = RoleAssignmentsDeleteRequest.builder()
                    .caseId(caseReference)
                    .userId(userId)
                    .roleNames(List.of(CREATOR.getRole()))
                    .build();
            roleAssignmentService.deleteRoleAssignments(List.of(deleteRequest));
        }
        if (applicationParams.getEnableCaseUsersDbSync()) {
            caseUserRepository.revokeAccess(Long.valueOf(caseDetails.getId()), userId, CREATOR.getRole());
        }
    }

    @Transactional
    public List<String> findCasesUserIdHasAccessTo(final String userId) {

        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            return roleAssignmentService.getCaseReferencesForAGivenUser(userId);
        } else {
            List<Long> usersCases = caseUserRepository.findCasesUserIdHasAccessTo(userId);
            if (usersCases.isEmpty()) {
                return List.of();
            } else {
                return caseDetailsRepository
                    .findCaseReferencesByIds(usersCases)
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
            }
        }
    }

    @Transactional
    public void updateUserAccess(CaseDetails caseDetails, CaseUser caseUser) {
        final Set<String> validCaseRoles = caseRoleRepository.getCaseRoles(caseDetails.getCaseTypeId());
        final Set<String> globalCaseRoles = GlobalCaseRole.all();
        final Set<String> targetCaseRoles = caseUser.getCaseRoles();

        validateCaseRoles(Sets.union(globalCaseRoles, validCaseRoles), targetCaseRoles);

        final String userId = caseUser.getUserId();

        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            // NB: `replaceExisting = true` uses RAS which does not need us to revokeRemoved or ignoreGranted.
            roleAssignmentService.createCaseRoleAssignments(caseDetails, userId, targetCaseRoles, true);
        }
        if (applicationParams.getEnableCaseUsersDbSync()) {
            final var caseId = Long.valueOf(caseDetails.getId());
            final List<String> currentCaseRoles = caseUserRepository.findCaseRoles(caseId, userId);

            grantAddedCaseRoles(userId, caseId, currentCaseRoles, targetCaseRoles);
            revokeRemovedCaseRoles(userId, caseId, currentCaseRoles, targetCaseRoles);
        }
    }

    @Transactional
    public void addCaseUserRoles(List<CaseAssignedUserRoleWithOrganisation> caseUserRoles) {

        Map<CaseDetails, List<CaseAssignedUserRoleWithOrganisation>> cauRolesByCaseDetails =
            getMapOfCaseAssignedUserRolesByCaseDetails(caseUserRoles);

        // load all existing case user roles upfront
        List<CaseAssignedUserRole> existingCaseUserRoles = findCaseUserRoles(cauRolesByCaseDetails);

        Map<String, Map<String, Long>> newUserCounts
            = getNewUserCountByCaseAndOrganisation(cauRolesByCaseDetails, existingCaseUserRoles);

        cauRolesByCaseDetails.forEach((caseDetails, requestedAssignments) -> {
            Map<String, Set<String>> caseRolesByUserIdAndCase = requestedAssignments.stream()
                // filter out existing case user roles
                .filter(caseUserRole ->
                        existingCaseUserRoles.stream()
                            .noneMatch(cauRole -> caseUserRole.getCaseDataId().equals(cauRole.getCaseDataId())
                                && caseUserRole.getUserId().equals(cauRole.getUserId())
                                && caseUserRole.getCaseRole().equalsIgnoreCase(cauRole.getCaseRole())))
                // group by UserID
                .collect(Collectors.groupingBy(
                    CaseAssignedUserRole::getUserId,
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        caseUserRole -> caseUserRole.stream()
                            .map(CaseAssignedUserRole::getCaseRole)
                            .collect(Collectors.toSet())
                    )));

                if (applicationParams.getEnableAttributeBasedAccessControl()) {
                    caseRolesByUserIdAndCase.forEach((userId, caseRoles) ->
                        // NB: `replaceExisting = false` uses RAS which needs us to filter out existing case user roles
                        //      to prevent duplicates being generated.  see filter above.
                        roleAssignmentService.createCaseRoleAssignments(caseDetails, userId, caseRoles, false)
                    );
                }
                if (applicationParams.getEnableCaseUsersDbSync()) {
                    Long caseId = Long.parseLong(caseDetails.getId());
                    caseRolesByUserIdAndCase.forEach((userId, caseRoles) ->
                        caseRoles.forEach(caseRole ->
                            caseUserRepository.grantAccess(caseId, userId, caseRole)));
                }
            }
        );

        newUserCounts.forEach((caseReference, orgNewUserCountMap) ->
            orgNewUserCountMap.forEach((organisationId, newUserCount) ->
                supplementaryDataRepository.incrementSupplementaryData(caseReference,
                    ORGS_ASSIGNED_USERS_PATH + organisationId, newUserCount)
            )
        );
    }

    @Transactional
    public void removeCaseUserRoles(List<CaseAssignedUserRoleWithOrganisation> caseUserRoles) {

        Map<CaseDetails, List<CaseAssignedUserRoleWithOrganisation>> cauRolesByCaseDetails =
            getMapOfCaseAssignedUserRolesByCaseDetails(caseUserRoles);

        // Ignore case user role mappings that DO NOT exist
        // NB: also required so they don't effect revoked user counts.
        Map<CaseDetails, List<CaseAssignedUserRoleWithOrganisation>> filteredCauRolesByCaseDetails =
            findAndFilterOnExistingCauRoles(cauRolesByCaseDetails);

        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            List<RoleAssignmentsDeleteRequest> deleteRequests = new ArrayList<>();

            // for each case
            filteredCauRolesByCaseDetails.forEach((caseDetails, requestedAssignments) -> {
                // group by user
                Map<String, List<String>> caseRolesByUserAndCase = requestedAssignments.stream()
                    .collect(Collectors.groupingBy(
                        CaseAssignedUserRoleWithOrganisation::getUserId,
                        Collectors.collectingAndThen(
                            Collectors.toList(),
                            roles -> roles.stream()
                                .map(CaseAssignedUserRoleWithOrganisation::getCaseRole)
                                .collect(Collectors.toList())
                        )));

                // for each user in current case: add list of all case-roles to revoke to the delete requests
                caseRolesByUserAndCase.forEach((userId, roleNames) ->
                    deleteRequests.add(RoleAssignmentsDeleteRequest.builder()
                        .caseId(caseDetails.getReferenceAsString())
                        .userId(userId)
                        .roleNames(roleNames)
                        .build()
                ));
            });

            // submit list of all delete requests from all cases
            roleAssignmentService.deleteRoleAssignments(deleteRequests);
        }
        if (applicationParams.getEnableCaseUsersDbSync()) {
            filteredCauRolesByCaseDetails.forEach((caseDetails, requestedAssignments) -> {
                    Long caseId = Long.parseLong(caseDetails.getId());
                    requestedAssignments.forEach(requestedAssignment ->
                        caseUserRepository.revokeAccess(caseId, requestedAssignment.getUserId(),
                            requestedAssignment.getCaseRole())
                    );
                }
            );
        }

        // determine counters after removal of requested mappings so that same function can be re-used
        // (i.e user still has an association to a case).
        Map<String, Map<String, Long>> removeUserCounts
            = getNewUserCountByCaseAndOrganisation(filteredCauRolesByCaseDetails, null);

        removeUserCounts.forEach((caseReference, orgNewUserCountMap) ->
            orgNewUserCountMap.forEach((organisationId, removeUserCount) ->
                supplementaryDataRepository.incrementSupplementaryData(caseReference,
                    ORGS_ASSIGNED_USERS_PATH + organisationId, Math.negateExact(removeUserCount))
            )
        );
    }

    private Map<CaseDetails, List<CaseAssignedUserRoleWithOrganisation>> findAndFilterOnExistingCauRoles(
        Map<CaseDetails, List<CaseAssignedUserRoleWithOrganisation>> cauRolesByCaseDetails
    ) {
        // find existing Case-User relationships and group by case reference
        Map<String, List<CaseAssignedUserRole>> existingCaseUserRolesByCaseReference =
            findCaseUserRoles(cauRolesByCaseDetails).stream()
                .collect(Collectors.groupingBy(
                    CaseAssignedUserRole::getCaseDataId,
                    Collectors.toList()
                ));

        return cauRolesByCaseDetails.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey,
                entry -> filterOnExistingCauRoles(
                    entry.getValue(),
                    existingCaseUserRolesByCaseReference.getOrDefault(
                        entry.getKey().getReferenceAsString(),
                        new ArrayList<>()
                    )
                )));
    }

    private List<CaseAssignedUserRoleWithOrganisation> filterOnExistingCauRoles(
        List<CaseAssignedUserRoleWithOrganisation> inputCauRoles,
        List<CaseAssignedUserRole> existingCauRoles) {
        return inputCauRoles.stream()
            .filter(cauRole -> existingCauRoles.stream()
                .anyMatch(entity -> entity.getCaseRole().equalsIgnoreCase(cauRole.getCaseRole())
                    && entity.getUserId().equalsIgnoreCase(cauRole.getUserId())
                    && entity.getCaseDataId().equalsIgnoreCase(cauRole.getCaseDataId())))
            .collect(Collectors.toList());
    }

    private List<Long> getCaseIdsFromCaseDetailsList(List<CaseDetails> caseDetailsList) {
        return caseDetailsList.stream()
            .map(CaseDetails::getId)
            .distinct()
            .map(Long::parseLong)
            .collect(Collectors.toList());
    }

    private List<String> getUserIdsFromMap(
        Map<CaseDetails, List<CaseAssignedUserRoleWithOrganisation>> cauRolesByCaseDetails
    ) {
        return cauRolesByCaseDetails.values().stream()
            .map(cauRoles -> cauRoles.stream()
                .map(CaseAssignedUserRole::getUserId)
                .collect(Collectors.toList())
            )
            .flatMap(List::stream)
            .distinct()
            .collect(Collectors.toList());
    }

    private Map<CaseDetails, List<CaseAssignedUserRoleWithOrganisation>> getMapOfCaseAssignedUserRolesByCaseDetails(
        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles
    ) {

        Map<CaseDetails, List<CaseAssignedUserRoleWithOrganisation>> cauRolesByCaseCaseDetails = new HashMap<>();

        List<Long> caseReferences = caseUserRoles.stream()
            .map(CaseAssignedUserRoleWithOrganisation::getCaseDataId)
            .distinct()
            .map(Long::parseLong)
            .collect(Collectors.toCollection(ArrayList::new));

        // create map of case references to case details
        Map<Long, CaseDetails> caseDetailsByReferences = getCaseDetailsList(caseReferences).stream()
            .collect(Collectors.toMap(CaseDetails::getReference, caseDetails -> caseDetails));

        // group roles by case reference
        Map<String, List<CaseAssignedUserRoleWithOrganisation>> cauRolesByCaseReference = caseUserRoles.stream()
            .collect(Collectors.groupingBy(CaseAssignedUserRoleWithOrganisation::getCaseDataId));

        // merge both maps to check we have found all cases
        cauRolesByCaseReference.forEach((key, roles) -> {
            final Long caseReference = Long.parseLong(key);
            if (caseDetailsByReferences.containsKey(caseReference)) {
                cauRolesByCaseCaseDetails.put(caseDetailsByReferences.get(caseReference), roles);
            } else {
                throw new CaseNotFoundException(key);
            }
        });

        return cauRolesByCaseCaseDetails;
    }

    private Map<String, Map<String, Long>> getNewUserCountByCaseAndOrganisation(
        Map<CaseDetails, List<CaseAssignedUserRoleWithOrganisation>> cauRolesByCaseDetails,
        List<CaseAssignedUserRole> existingCaseUserRoles
    ) {
        Map<String, Map<String, Long>> result = new HashMap<>();

        Map<CaseDetails, List<CaseAssignedUserRoleWithOrganisation>> caseUserRolesWhichHaveAnOrgId =
            cauRolesByCaseDetails.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                // filter out no organisation_id and [CREATOR] case role
                .filter(caseUserRole ->
                        StringUtils.isNoneBlank(caseUserRole.getOrganisationId())
                            && !caseUserRole.getCaseRole().equalsIgnoreCase(CREATOR.getRole()))
                .collect(Collectors.toList())))
            // filter cases that have no remaining roles
            .entrySet().stream().filter(e -> !e.getValue().isEmpty())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // if empty list this processing is not required
        if (caseUserRolesWhichHaveAnOrgId.isEmpty()) {
            return result; // exit with empty map
        }

        // if not preloaded the existing/current case roles get a fresh snapshot now
        if (existingCaseUserRoles == null) {
            existingCaseUserRoles = findCaseUserRoles(caseUserRolesWhichHaveAnOrgId);
        }

        // find existing Case-User relationships for all the relevant cases + users found
        Map<Long, List<String>> existingCaseUserRelationships =
            existingCaseUserRoles.stream()
                // filter out [CREATOR] case role
                .filter(caseUserRole -> !caseUserRole.getCaseRole().equalsIgnoreCase(CREATOR.getRole()))
                .collect(Collectors.groupingBy(
                    caseUserRole -> Long.parseLong(caseUserRole.getCaseDataId()),
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        caseUserRole -> caseUserRole.stream()
                            .map(CaseAssignedUserRole::getUserId)
                            .distinct().collect(Collectors.toList())
                    )));

        // for each case: count new Case-User relationships by Organisation
        caseUserRolesWhichHaveAnOrgId.forEach((caseDetails, requestedAssignments) -> {
            List<String> existingUsersForCase
                = existingCaseUserRelationships.getOrDefault(caseDetails.getReference(), new ArrayList<>());

            Map<String, Long> relationshipCounts = requestedAssignments.stream()
                // filter out any existing relationships
                .filter(cauRole -> !existingUsersForCase.contains(cauRole.getUserId()))
                // count unique users for each organisation
                .collect(Collectors.groupingBy(
                    CaseAssignedUserRoleWithOrganisation::getOrganisationId,
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        cauRolesForOrganisation -> cauRolesForOrganisation.stream()
                            .map(CaseAssignedUserRoleWithOrganisation::getUserId).distinct().count())));

            // skip if no organisations have any relationships
            if (!relationshipCounts.isEmpty()) {
                result.put(caseDetails.getReferenceAsString(), relationshipCounts);
            }
        });

        return result;
    }

    public List<CaseAssignedUserRole> findCaseUserRoles(List<Long> caseReferences, List<String> userIds) {

        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            final var caseIds = caseReferences.stream().map(String::valueOf).collect(Collectors.toList());
            return roleAssignmentService.findRoleAssignmentsByCasesAndUsers(caseIds, userIds);
        } else {
            List<CaseDetails> caseDetailsList = getCaseDetailsList(caseReferences);

            if (caseDetailsList.isEmpty()) {
                return Lists.newArrayList();
            }

            List<Long> caseIds = getCaseIdsFromCaseDetailsList(caseDetailsList);
            List<CaseUserEntity> caseUserEntities = caseUserRepository.findCaseUserRoles(caseIds, userIds);
            return getCaseAssignedUserRolesFromCaseUserEntities(caseUserEntities, caseDetailsList);
        }
    }

    private List<CaseAssignedUserRole> findCaseUserRoles(
        Map<CaseDetails, List<CaseAssignedUserRoleWithOrganisation>> cauRolesByCaseDetails
    ) {
        List<CaseDetails> caseDetailsList = new ArrayList<>(cauRolesByCaseDetails.keySet());
        List<String> userIds = getUserIdsFromMap(cauRolesByCaseDetails);

        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            final var caseIds = caseDetailsList.stream()
                .map(CaseDetails::getReferenceAsString).collect(Collectors.toList());
            return roleAssignmentService.findRoleAssignmentsByCasesAndUsers(caseIds, userIds);
        } else {
            List<Long> caseIds = getCaseIdsFromCaseDetailsList(caseDetailsList);
            List<CaseUserEntity> caseUserEntities = caseUserRepository.findCaseUserRoles(caseIds, userIds);
            return getCaseAssignedUserRolesFromCaseUserEntities(caseUserEntities, caseDetailsList);
        }
    }

    private List<CaseAssignedUserRole> getCaseAssignedUserRolesFromCaseUserEntities(
        List<CaseUserEntity> caseUserEntities,
        List<CaseDetails> caseDetailsList
    ) {
        Map<String, Long> caseReferenceAndIds = caseDetailsList.stream()
            .collect(Collectors.toMap(CaseDetails::getId, CaseDetails::getReference));

        return caseUserEntities.stream()
            .map(cue -> new CaseAssignedUserRole(
                String.valueOf(caseReferenceAndIds.get(String.valueOf(cue.getCasePrimaryKey().getCaseDataId()))),
                cue.getCasePrimaryKey().getUserId(),
                cue.getCasePrimaryKey().getCaseRole()))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<CaseDetails> getCaseDetailsList(List<Long> caseReferences) {
        return caseReferences.stream()
            .map(caseReference -> {
                Optional<CaseDetails> caseDetails = caseDetailsRepository.findByReference(null, caseReference);
                return caseDetails.orElse(null);
            }).filter(Objects::nonNull)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private void validateCaseRoles(Set<String> validCaseRoles, Set<String> targetCaseRoles) {
        targetCaseRoles.stream()
                .filter(role -> !validCaseRoles.contains(role))
                .findFirst()
                .ifPresent(role -> {
                    throw new InvalidCaseRoleException(role);
                });
    }

    private void grantAddedCaseRoles(String userId,
                                     Long caseId,
                                     List<String> currentCaseRoles,
                                     Set<String> targetCaseRoles) {
        targetCaseRoles.stream()
            .filter(targetRole -> !currentCaseRoles.contains(targetRole))
            .forEach(targetRole -> caseUserRepository.grantAccess(caseId, userId, targetRole));
    }

    private void revokeRemovedCaseRoles(String userId,
                                        Long caseId,
                                        List<String> currentCaseRoles,
                                        Set<String> targetCaseRoles) {
        currentCaseRoles.stream()
            .filter(currentRole -> !targetCaseRoles.contains(currentRole))
            .forEach(currentRole -> caseUserRepository.revokeAccess(caseId,
                userId,
                currentRole));
    }
}
