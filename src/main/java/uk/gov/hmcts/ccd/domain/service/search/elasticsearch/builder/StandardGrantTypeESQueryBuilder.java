package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class StandardGrantTypeESQueryBuilder extends GrantTypeESQueryBuilder {

    StandardGrantTypeESQueryBuilder(AccessControlService accessControlService,
                                    CaseDataAccessControl caseDataAccessControl,
                                    ApplicationParams applicationParams) {
        super(accessControlService, caseDataAccessControl, applicationParams);
    }

    @Override
    protected GrantType getGrantType() {
        return GrantType.STANDARD;
    }

    /**
     * Builds the STANDARD grant type query using the new Elasticsearch API client.
     */
    public Query createQuery(List<RoleAssignment> roleAssignments, CaseTypeDefinition caseTypeDefinition) {
        List<Query> standardQueries = roleAssignments.stream()
            .filter(role -> GrantType.STANDARD.equals(role.getGrantType()))
            .map(role -> Query.of(q -> q
                .term(t -> t
                    .field("access.grant")
                    .value(role.getRoleName())  // Adjust this field depending on your mapping
                )
            ))
            .collect(Collectors.toList());

        return Query.of(q -> q.bool(b -> b)); // empty bool query

    }
}
