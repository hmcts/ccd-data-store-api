package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import org.apache.commons.lang3.StringUtils;
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
    public Query createQuery(List<RoleAssignment> roleAssignments, CaseTypeDefinition caseType) {
        Supplier<Stream<RoleAssignment>> streamSupplier = filterGrantTypeRoleAssignments(roleAssignments);

        return getCaseReferenceQuery(streamSupplier)
            .orElse(Query.of(q -> q.bool(b -> b))); // return empty bool query if no case refs
    }

    private Optional<Query> getCaseReferenceQuery(Supplier<Stream<RoleAssignment>> streamSupplier) {
        Set<String> caseReferences = streamSupplier.get()
            .filter(role -> role.getAttributes() != null)
            .map(role -> role.getAttributes().getCaseId())
            .filter(Objects::nonNull)
            .map(Optional::get)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());

        if (caseReferences.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(Query.of(q -> q.terms(t -> t
            .field(REFERENCE_FIELD_COL + KEYWORD)
            .terms(TermsQueryField.of(f -> f.value(
                caseReferences.stream()
                    .map(FieldValue::of)
                    .collect(Collectors.toList())
            )))
        )));
    }
}
