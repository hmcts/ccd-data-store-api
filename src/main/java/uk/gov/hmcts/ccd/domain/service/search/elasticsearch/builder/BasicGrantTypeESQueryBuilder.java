package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.stream.Stream;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;

@Component
public class BasicGrantTypeESQueryBuilder implements GrantTypeESQueryBuilder {

    @Override
    public BoolQueryBuilder createQuery(List<RoleAssignment> roleAssignments) {
        Stream<RoleAssignment> roleAssignmentStream = roleAssignments.stream()
            .filter(roleAssignment -> GrantType.BASIC.name().equals(roleAssignment.getGrantType()));

        BoolQueryBuilder basicQuery =  QueryBuilders.boolQuery();
        Lists.newArrayList(createClassification(roleAssignmentStream))
            .stream()
            .filter(query -> query.isPresent())
            .map(query -> query.get())
            .forEach(query -> basicQuery.must(query));
        return basicQuery;
    }
}
