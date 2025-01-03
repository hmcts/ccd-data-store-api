package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.REFERENCE_FIELD_COL;

@Component
public class ExcludedGrantTypeESQueryBuilder extends GrantTypeESQueryBuilder {

    ExcludedGrantTypeESQueryBuilder(AccessControlService accessControlService,
                                    CaseDataAccessControl caseDataAccessControl,
                                    ApplicationParams applicationParams) {
        super(accessControlService, caseDataAccessControl, applicationParams);
    }

    @Override
    protected GrantType getGrantType() {
        return GrantType.EXCLUDED;
    }

    @Override
    public BoolQueryBuilder createQuery(List<RoleAssignment> roleAssignments,
                                        CaseTypeDefinition caseType) {
        Supplier<Stream<RoleAssignment>> streamSupplier = filterGrantTypeRoleAssignments(roleAssignments);

        BoolQueryBuilder excludedQuery = QueryBuilders.boolQuery();
        getCaseReferences(streamSupplier).ifPresent(excludedQuery::must);
        return excludedQuery;
    }

    @SuppressWarnings("java:S2789")
    private Optional<TermsQueryBuilder> getCaseReferences(Supplier<Stream<RoleAssignment>> streamSupplier) {
        Set<String> caseReferences = streamSupplier.get()
            .filter(roleAssignment -> roleAssignment.getAttributes() != null)
            .map(roleAssignment -> roleAssignment.getAttributes().getCaseId())
            .filter(Objects::nonNull)
            .map(Optional::get)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());

        if (!caseReferences.isEmpty()) {
            return Optional.of(QueryBuilders.termsQuery(REFERENCE_FIELD_COL + KEYWORD, caseReferences));
        }
        return Optional.empty();
    }
}
