package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.JURISDICTION_FIELD_KEYWORD_COL;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.LOCATION;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.REFERENCE_FIELD_COL;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.REGION;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.SECURITY_CLASSIFICATION_FIELD_COL;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.STATE_FIELD_COL;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

public abstract class GrantTypeESQueryBuilder {

    protected AccessControlService accessControlService;
    protected CaseDataAccessControl caseDataAccessControl;

    protected GrantTypeESQueryBuilder(AccessControlService accessControlService,
                                      CaseDataAccessControl caseDataAccessControl) {
        this.accessControlService = accessControlService;
        this.caseDataAccessControl = caseDataAccessControl;
    }

    protected abstract GrantType getGrantType();

    public BoolQueryBuilder createQuery(List<RoleAssignment> roleAssignments,
                                        CaseTypeDefinition caseType) {
        List<CaseStateDefinition> caseStates = caseType == null ? Lists.newArrayList() : caseType.getStates();
        Supplier<Stream<RoleAssignment>> streamSupplier = filterGrantTypeRoleAssignments(roleAssignments);
        BoolQueryBuilder query = QueryBuilders.boolQuery();

        streamSupplier.get()
            .filter(roleAssignment -> roleAssignment.getAttributes() != null)
            .map(roleAssignment -> {
                BoolQueryBuilder innerQuery = QueryBuilders.boolQuery();

                Set<AccessProfile> accessProfiles = getAccessProfiles(roleAssignment, caseType);
                Optional<TermsQueryBuilder> stateQuery = createCaseStateQuery(caseStates, accessProfiles);
                if (stateQuery.isPresent()) {
                    if (stateQuery.get().values().size() != caseStates.size()) {
                        innerQuery.must(stateQuery.get());
                    }
                } else {
                    return null;
                }

                addExactMatchQueryForOptionalAttribute(roleAssignment.getAttributes().getJurisdiction(),
                    innerQuery, JURISDICTION_FIELD_KEYWORD_COL);
                addExactMatchQueryForOptionalAttribute(roleAssignment.getAttributes().getRegion(),
                    innerQuery, REGION);
                addExactMatchQueryForOptionalAttribute(roleAssignment.getAttributes().getLocation(),
                    innerQuery, LOCATION);
                addExactMatchQueryForOptionalAttribute(roleAssignment.getAttributes().getCaseId(),
                    innerQuery, REFERENCE_FIELD_COL);

                String classification = roleAssignment.getClassification();
                if (StringUtils.isNotBlank(classification)) {
                    createClassification(roleAssignment).ifPresent(innerQuery::must);
                }

                return innerQuery;
            }).filter(Objects::nonNull).forEach(query::should);

        return query;
    }

    private void addExactMatchQueryForOptionalAttribute(Optional<String> attribute,
                                                        BoolQueryBuilder parentQuery,
                                                        String matchName) {
        if (attribute != null && StringUtils.isNotBlank(attribute.orElse(""))) {
            parentQuery.must(QueryBuilders.termQuery(matchName + ".keyword", attribute.get()));
        }
    }

    protected Supplier<Stream<RoleAssignment>> filterGrantTypeRoleAssignments(List<RoleAssignment> roleAssignments) {
        return () -> roleAssignments.stream()
            .filter(roleAssignment -> getGrantType().name().equals(roleAssignment.getGrantType()));
    }

    private Optional<TermsQueryBuilder> createClassification(RoleAssignment roleAssignment) {
        String raClassification = roleAssignment.getClassification();

        if (StringUtils.isNotBlank(raClassification)) {
            List<String> classificationsList;
            try {
                classificationsList = SecurityClassification
                    .valueOf(raClassification).getClassificationsLowerOrEqualTo();
            } catch (IllegalArgumentException ex) {
                return Optional.empty();
            }

            if (!classificationsList.isEmpty()) {
                return Optional.of(QueryBuilders.termsQuery(SECURITY_CLASSIFICATION_FIELD_COL, classificationsList));
            }
        }

        return Optional.empty();
    }

    private Optional<TermsQueryBuilder> createCaseStateQuery(List<CaseStateDefinition> caseStates,
                                                             Set<AccessProfile> accessProfiles) {
        List<String> readableCaseStates = accessControlService
            .filterCaseStatesByAccess(caseStates, accessProfiles, CAN_READ)
            .stream()
            .map(CaseStateDefinition::getId)
            .collect(Collectors.toList());

        if (!readableCaseStates.isEmpty()) {
            return Optional.of(QueryBuilders.termsQuery(STATE_FIELD_COL, readableCaseStates));
        }

        return Optional.empty();
    }

    private Set<AccessProfile> getAccessProfiles(RoleAssignment roleAssignment,
                                                 CaseTypeDefinition caseTypeDefinition) {
        return caseDataAccessControl.filteredAccessProfiles(List.of(roleAssignment), caseTypeDefinition, false);
    }
}
