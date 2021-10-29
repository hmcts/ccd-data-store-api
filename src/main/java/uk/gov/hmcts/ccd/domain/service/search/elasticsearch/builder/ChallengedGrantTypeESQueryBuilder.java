package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

@Component
public class ChallengedGrantTypeESQueryBuilder implements GrantTypeESQueryBuilder {

    private AccessControlService accessControlService;

    @Autowired
    public ChallengedGrantTypeESQueryBuilder(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @Override
    public BoolQueryBuilder createQuery(List<RoleAssignment> roleAssignments, List<CaseStateDefinition> caseStates) {
        Supplier<Stream<RoleAssignment>> streamSupplier = () -> roleAssignments.stream()
            .filter(roleAssignment -> GrantType.CHALLENGED.name().equals(roleAssignment.getGrantType()));

        BoolQueryBuilder challengedQuery =  QueryBuilders.boolQuery();
        Lists.newArrayList(createClassification(streamSupplier.get()),
            createCaseStateQuery(streamSupplier, accessControlService, caseStates),
            getJurisdictions(streamSupplier))
            .stream()
            .filter(query -> query.isPresent())
            .map(query -> query.get())
            .forEach(query -> challengedQuery.must(query));
        return challengedQuery;
    }
}
