package uk.gov.hmcts.ccd.domain.service.caseaccess;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.util.set.Sets;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseRoleRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseRoleRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserEntity;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.supplementarydata.SupplementaryDataRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.InvalidCaseRoleException;
import uk.gov.hmcts.ccd.v2.external.domain.CaseUser;

import static uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole.CREATOR;

@Service
public class CaseAccessOperation {

    private final CaseUserRepository caseUserRepository;
    private final CaseDetailsRepository caseDetailsRepository;
    private final CaseRoleRepository caseRoleRepository;
    private final SupplementaryDataRepository supplementaryDataRepository;

    public CaseAccessOperation(final @Qualifier(CachedCaseUserRepository.QUALIFIER)  CaseUserRepository caseUserRepository,
                               @Qualifier(CachedCaseDetailsRepository.QUALIFIER) final CaseDetailsRepository caseDetailsRepository,
                               @Qualifier(CachedCaseRoleRepository.QUALIFIER) CaseRoleRepository caseRoleRepository,
                               @Qualifier("default") SupplementaryDataRepository supplementaryDataRepository) {
        this.caseUserRepository = caseUserRepository;
        this.caseDetailsRepository = caseDetailsRepository;
        this.caseRoleRepository = caseRoleRepository;
        this.supplementaryDataRepository = supplementaryDataRepository;
    }

    @Transactional
    public void grantAccess(final String jurisdictionId, final String caseReference, final String userId) {
        final Optional<CaseDetails> maybeCase = caseDetailsRepository.findByReference(jurisdictionId,
                                                                                      Long.valueOf(caseReference));

        final CaseDetails caseDetails = maybeCase.orElseThrow(() -> new CaseNotFoundException(caseReference));
        caseUserRepository.grantAccess(Long.valueOf(caseDetails.getId()), userId, CREATOR.getRole());
    }

    @Transactional
    public void revokeAccess(final String jurisdictionId, final String caseReference, final String userId) {
        final Optional<CaseDetails> maybeCase = caseDetailsRepository.findByReference(jurisdictionId,
                                                                                      Long.valueOf(caseReference));
        final CaseDetails caseDetails = maybeCase.orElseThrow(() -> new CaseNotFoundException(caseReference));
        caseUserRepository.revokeAccess(Long.valueOf(caseDetails.getId()), userId, CREATOR.getRole());
    }

    public List<String> findCasesUserIdHasAccessTo(final String userId) {
        return caseUserRepository.findCasesUserIdHasAccessTo(userId)
                                 .stream()
                                 .map(databaseId -> caseDetailsRepository.findById(databaseId).getReference() + "")
                                 .collect(Collectors.toList());
    }

    @Transactional
    public void updateUserAccess(CaseDetails caseDetails, CaseUser caseUser) {
        final Set<String> validCaseRoles = caseRoleRepository.getCaseRoles(caseDetails.getCaseTypeId());
        final Set<String> globalCaseRoles = GlobalCaseRole.all();
        final Set<String> targetCaseRoles = caseUser.getCaseRoles();

        validateCaseRoles(Sets.union(globalCaseRoles, validCaseRoles), targetCaseRoles);

        final Long caseId = new Long(caseDetails.getId());
        final String userId = caseUser.getUserId();
        final List<String> currentCaseRoles = caseUserRepository.findCaseRoles(caseId, userId);

        grantAddedCaseRoles(userId, caseId, currentCaseRoles, targetCaseRoles);
        revokeRemovedCaseRoles(userId, caseId, currentCaseRoles, targetCaseRoles);
    }

    @Transactional
    public void addCaseUserRoles(List<CaseAssignedUserRole> caseUserRoles) {

        Map<Long, List<CaseAssignedUserRole>> cauRolesByCaseId = getMapOfCaseAssignedUserRolesByCaseId(caseUserRoles);

        Map<String, Map<String, Object>> newUserCounts = getNewUserCountByCaseAndOrganisation(cauRolesByCaseId);

        cauRolesByCaseId.forEach((caseId, roles) ->
            roles.forEach(caseUserRole ->
                caseUserRepository.grantAccess(caseId, caseUserRole.getUserId(), caseUserRole.getCaseRole()))
        );

        if (newUserCounts != null) {
            newUserCounts.forEach((caseReference, orgNewUserCountMap) ->
                orgNewUserCountMap.forEach((organisationId, newUserCount) ->
                    supplementaryDataRepository.incrementSupplementaryData(caseReference, "orgs_assigned_users." + organisationId, newUserCount)
                ));
        }
    }

    private Map<Long, List<CaseAssignedUserRole>> getMapOfCaseAssignedUserRolesByCaseId(List<CaseAssignedUserRole> caseUserRoles) {

        Map<Long, List<CaseAssignedUserRole>> cauRolesByCaseId = new HashMap<>();

        List<Long> caseReferences = caseUserRoles.stream()
            .map(CaseAssignedUserRole::getCaseDataId)
            .distinct()
            .map(Long::parseLong)
            .collect(Collectors.toCollection(ArrayList::new));

        // create map of case references to case IDs
        Map<Long, String> caseIdAndReferences = getCaseDetailsList(caseReferences).stream()
            .collect(Collectors.toMap(CaseDetails::getReference, CaseDetails::getId));

        // group roles by case reference
        Map<String, List<CaseAssignedUserRole>> cauRolesByCaseReference = caseUserRoles.stream()
            .collect(Collectors.groupingBy(CaseAssignedUserRole::getCaseDataId));

        // merge both maps to check we have found all cases
        cauRolesByCaseReference.forEach((key, roles) -> {
            final Long caseReference = Long.parseLong(key);
            if (caseIdAndReferences.containsKey(caseReference)) {
                final Long caseId = Long.parseLong(caseIdAndReferences.get(caseReference));
                cauRolesByCaseId.put(caseId, roles);
            } else {
                throw new CaseNotFoundException(key);
            }
        });

        return cauRolesByCaseId;
    }

    private Map<String, Map<String, Object>> getNewUserCountByCaseAndOrganisation(Map<Long, List<CaseAssignedUserRole>> cauRolesByCaseId) {
        Map<String, Map<String, Object>> result = new HashMap<>();

        Map<Long, List<CaseAssignedUserRole>> caseUserRolesWithOrganisations = cauRolesByCaseId.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                // filter out no organisation_id
                .filter(caseUserRole -> StringUtils.isNoneBlank(caseUserRole.getOrganisationId()))
                .collect(Collectors.toList())))
            // filter cases that have no remaining roles
            .entrySet().stream().filter(e -> !e.getValue().isEmpty())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // if empty list this processing is not required
        if (caseUserRolesWithOrganisations.isEmpty()) {
            return null;
        }

        // get distinct list of remaining caseIds
        List<Long> caseIds = new ArrayList<>(caseUserRolesWithOrganisations.keySet());

        // get distinct list of user ids
        List<String> userIds = caseUserRolesWithOrganisations.values().stream()
            .map(roles -> roles.stream().map(CaseAssignedUserRole::getUserId).collect(Collectors.toList()))
            .flatMap(List::stream)
            .distinct()
            .collect(Collectors.toList());

        // find existing Case-User relationships for all the relevant cases + users found
        Map<Long, List<String>> existingCaseUserRelationships =
            caseUserRepository.findCaseUserRoles(caseIds, userIds).stream()
                .collect(Collectors.groupingBy(
                    caseUserEntity -> caseUserEntity.getCasePrimaryKey().getCaseDataId(),
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        userRoles -> userRoles.stream()
                            .map(caseUserEntity -> caseUserEntity.getCasePrimaryKey().getUserId())
                            .distinct().collect(Collectors.toList())
                    )));

        // for each case: count new Case-User relationships by Organisation
        caseUserRolesWithOrganisations.forEach((caseId, roles) -> {
            List<String> existingUsersForCase = existingCaseUserRelationships.getOrDefault(caseId, new ArrayList<>());

            Map<String, Object> newRelationshipCounts = roles.stream()
                // filter out any existing relationships
                .filter(cauRole -> !existingUsersForCase.contains(cauRole.getUserId()))
                // count unique users for each organisation
                .collect(Collectors.groupingBy(
                    CaseAssignedUserRole::getOrganisationId,
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        cauRolesForOrganisation -> cauRolesForOrganisation.stream()
                            .map(CaseAssignedUserRole::getUserId).distinct().count())));

            // skip if no organisations have any new relationships
            if (!newRelationshipCounts.isEmpty()) {
                result.put(roles.get(0).getCaseDataId(), newRelationshipCounts);
            }
        });

        return result;
    }

    public List<CaseAssignedUserRole> findCaseUserRoles(List<Long> caseReferences, List<String> userIds) {
        Map<String, Long> caseReferenceAndIds = getCaseDetailsList(caseReferences).stream()
            .collect(Collectors.toMap(CaseDetails::getId, CaseDetails::getReference));

        if (caseReferenceAndIds.isEmpty()) {
            return Lists.newArrayList();
        }
        List<Long> caseIds = caseReferenceAndIds.keySet().stream().map(Long::valueOf).collect(Collectors.toList());
        List<CaseUserEntity> caseUserEntities = caseUserRepository.findCaseUserRoles(caseIds, userIds);
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
