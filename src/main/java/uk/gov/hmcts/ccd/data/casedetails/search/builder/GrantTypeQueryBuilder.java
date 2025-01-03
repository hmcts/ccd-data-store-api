package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.SearchRoleAssignment;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Slf4j
public abstract class GrantTypeQueryBuilder {

    private final AccessControlService accessControlService;
    private final CaseDataAccessControl caseDataAccessControl;
    private final ApplicationParams applicationParams;

    protected GrantTypeQueryBuilder(AccessControlService accessControlService,
                                    CaseDataAccessControl caseDataAccessControl,
                                    ApplicationParams applicationParams) {
        this.accessControlService = accessControlService;
        this.caseDataAccessControl = caseDataAccessControl;
        this.applicationParams = applicationParams;
    }

    protected abstract GrantType getGrantType();

    protected Supplier<Stream<RoleAssignment>> filterGrantTypeRoleAssignments(List<RoleAssignment> roleAssignments) {
        return () -> roleAssignments.stream()
            .filter(roleAssignment -> getGrantType().name().equals(roleAssignment.getGrantType()));
    }

    /**
     * Groups role assignments by those which are "similar" in the context of building a search query
     * i.e. *most* of the relevant values to search match in each group.
     * @param roleAssignments Role assignments to group
     * @return Grouped role assignments for search. Note that case reference is ignored in the grouping, so
     *      SearchRoleAssignments in the same group can have different case reference values.
     */
    protected Map<Integer, List<SearchRoleAssignment>> getGroupedSearchRoleAssignments(
        List<RoleAssignment> roleAssignments) {
        return filterGrantTypeRoleAssignments(roleAssignments).get()
            .map(SearchRoleAssignment::new)
            .collect(Collectors.groupingBy(SearchRoleAssignment::hashCode));
    }

    protected List<CaseStateDefinition> getStatesForCaseType(CaseTypeDefinition caseType) {
        return caseType == null ? newArrayList() : caseType.getStates();
    }

    protected List<String> getClassifications(SearchRoleAssignment searchRoleAssignment) {
        String classification = searchRoleAssignment.getSecurityClassification();
        if (StringUtils.isNotBlank(classification)) {
            try {
                return SecurityClassification
                    .valueOf(classification).getClassificationsLowerOrEqualTo();
            } catch (IllegalArgumentException ex) {
                log.warn("Found unknown classification '{}' in role assignment; ignored", classification);
            }
        }
        return Lists.newArrayList();
    }

    protected boolean allRoleAssignmentsHaveCaseReference(List<SearchRoleAssignment> searchRoleAssignments) {
        return searchRoleAssignments.stream().allMatch(SearchRoleAssignment::hasCaseReference);
    }

    protected Set<String> getReadableCaseStates(SearchRoleAssignment searchRoleAssignment,
                                                List<CaseStateDefinition> allCaseStates,
                                                CaseTypeDefinition caseType) {
        Set<AccessProfile> accessProfiles = getAccessProfiles(searchRoleAssignment.getRoleAssignment(), caseType);
        return accessControlService
            .filterCaseStatesByAccess(allCaseStates, accessProfiles, CAN_READ)
            .stream()
            .map(CaseStateDefinition::getId)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    protected Set<AccessProfile> getAccessProfiles(RoleAssignment roleAssignment,
                                                 CaseTypeDefinition caseTypeDefinition) {
        return caseDataAccessControl.filteredAccessProfiles(List.of(roleAssignment), caseTypeDefinition, false);
    }

    protected List<String> getCaseAccessCategories(RoleAssignment roleAssignment, CaseTypeDefinition caseType) {
        Set<AccessProfile> accessProfiles = getAccessProfiles(roleAssignment, caseType);

        if (ignoreCaseAccessCategoryQuery(accessProfiles)) {
            return Lists.newArrayList();
        }
        return accessProfiles.stream()
            .filter(ap -> ap.getCaseAccessCategories() != null)
            .flatMap(ap -> Arrays.stream(ap.getCaseAccessCategories().split(",")))
            .collect(Collectors.toList());
    }

    protected ApplicationParams getApplicationParams() {
        return applicationParams;
    }

    private boolean ignoreCaseAccessCategoryQuery(Set<AccessProfile> accessProfiles) {
        return accessProfiles.stream()
            .anyMatch(ap -> ap.getCaseAccessCategories() == null);
    }
}
