package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

public abstract class GrantTypeQueryBuilder {

    public static final String OR = " OR ";

    public static final String AND_NOT = " AND NOT ";

    private final AccessControlService accessControlService;

    private final CaseDataAccessControl caseDataAccessControl;

    public static final String QUERY_WRAPPER = "( %s )";

    public static final String QUERY = "%s in (:%s)";

    public static final String EMPTY = "";

    public static final String SECURITY_CLASSIFICATION = "security_classification";

    public static final String STATES = "state";

    public static final String JURISDICTION = "jurisdiction";

    public static final String REFERENCE = "reference";

    public static final String LOCATION = "data" + " #>> '{caseManagementLocation,baseLocation}'";

    public static final String REGION = "data" + " #>> '{caseManagementLocation,region}'";

    public static final String AND = " AND ";

    public static final String CASE_STATES_PARAM = "states_%s_%s";

    public static final String CLASSIFICATIONS_PARAM = "classifications_%s_%s";

    protected GrantTypeQueryBuilder(AccessControlService accessControlService,
                                      CaseDataAccessControl caseDataAccessControl) {
        this.accessControlService = accessControlService;
        this.caseDataAccessControl = caseDataAccessControl;
    }

    protected abstract GrantType getGrantType();

    @SuppressWarnings("java:S2789")
    public String createQuery(List<RoleAssignment> roleAssignments,
                              Map<String, Object> params,
                              CaseTypeDefinition caseType) {
        String paramName = getGrantType().name().toLowerCase();
        List<CaseStateDefinition> caseStates = caseType == null ? Lists.newArrayList() : caseType.getStates();
        Supplier<Stream<RoleAssignment>> streamSupplier = filterGrantTypeRoleAssignments(roleAssignments);
        AtomicInteger index = new AtomicInteger();
        return streamSupplier.get()
            .filter(roleAssignment -> roleAssignment.getAttributes() != null)
            .map(roleAssignment -> {

                String innerQuery =  EMPTY;
                int count = index.incrementAndGet();

                Set<AccessProfile> accessProfiles = getAccessProfiles(roleAssignment, caseType);
                List<String> raCaseStates = getCaseStates(caseStates, accessProfiles);
                if (raCaseStates.size() > 0) {
                    String statesParam = String.format(CASE_STATES_PARAM, count, paramName);
                    params.put(statesParam, raCaseStates);
                    innerQuery = String.format(QUERY, STATES, statesParam);
                } else {
                    return innerQuery;
                }

                Optional<String> jurisdiction = roleAssignment.getAttributes().getJurisdiction();
                if (jurisdiction != null && jurisdiction.isPresent() && StringUtils.isNotBlank(jurisdiction.get())) {
                    innerQuery = innerQuery + getOperator(innerQuery, AND)
                        + String.format("%s='%s'", JURISDICTION, jurisdiction.get());
                }

                Optional<String> region = roleAssignment.getAttributes().getRegion();
                if (region != null && region.isPresent() && StringUtils.isNotBlank(region.get())) {
                    innerQuery = innerQuery + getOperator(innerQuery, AND)
                        + String.format("%s='%s'", REGION, region.get());
                }

                Optional<String> location = roleAssignment.getAttributes().getLocation();
                if (location != null && location.isPresent() && StringUtils.isNotBlank(location.get())) {
                    innerQuery = innerQuery + getOperator(innerQuery, AND)
                        + String.format("%s='%s'", LOCATION, location.get());
                }

                Optional<String> reference = roleAssignment.getAttributes().getCaseId();
                if (reference != null && reference.isPresent() && StringUtils.isNotBlank(reference.get())) {
                    innerQuery = innerQuery + getOperator(innerQuery, AND)
                        + String.format("%s='%s'", REFERENCE, reference.get());
                }

                String classification = roleAssignment.getClassification();
                if (StringUtils.isNotBlank(classification)) {
                    List<String> classifications = getClassifications(roleAssignment);
                    if (classifications.size() > 0) {
                        String classificationsParam = String.format(CLASSIFICATIONS_PARAM, count, paramName);
                        params.put(classificationsParam, classifications);
                        innerQuery = innerQuery + getOperator(innerQuery, AND)
                            + String.format(QUERY, SECURITY_CLASSIFICATION, classificationsParam);
                    }
                }

                return StringUtils.isNotBlank(innerQuery) ? String.format(QUERY_WRAPPER, innerQuery) : innerQuery;
            }).filter(strQuery -> !StringUtils.isEmpty(strQuery)).collect(Collectors.joining(" OR "));
    }

    protected Supplier<Stream<RoleAssignment>> filterGrantTypeRoleAssignments(List<RoleAssignment> roleAssignments) {
        return () -> roleAssignments.stream()
            .filter(roleAssignment -> getGrantType().name().equals(roleAssignment.getGrantType()));
    }

    private List<String> getClassifications(RoleAssignment roleAssignment) {
        String raClassification = roleAssignment.getClassification();
        if (StringUtils.isNotBlank(raClassification)) {
            try {
                return SecurityClassification
                    .valueOf(raClassification).getClassificationsLowerOrEqualTo();
            } catch (IllegalArgumentException ex) {
                return Lists.newArrayList();
            }
        }
        return Lists.newArrayList();
    }

    private List<String> getCaseStates(List<CaseStateDefinition> caseStates,
                                       Set<AccessProfile> accessProfiles) {
        return accessControlService
            .filterCaseStatesByAccess(caseStates, accessProfiles, CAN_READ)
            .stream()
            .map(CaseStateDefinition::getId)
            .collect(Collectors.toList());
    }

    private Set<AccessProfile> getAccessProfiles(RoleAssignment roleAssignment,
                                                 CaseTypeDefinition caseTypeDefinition) {
        return caseDataAccessControl.filteredAccessProfiles(List.of(roleAssignment), caseTypeDefinition, false);
    }

    public String getOperator(String query, String operator) {
        if (StringUtils.isNotBlank(query)) {
            return operator;
        }
        return EMPTY;
    }
}
