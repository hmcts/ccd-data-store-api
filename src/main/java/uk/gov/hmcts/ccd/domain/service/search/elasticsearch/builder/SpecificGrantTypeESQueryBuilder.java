package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;

@Component
public class SpecificGrantTypeESQueryBuilder implements GrantTypeESQueryBuilder {

    @Override
    public BoolQueryBuilder createQuery(List<RoleAssignment> roleAssignments) {
        Supplier<Stream<RoleAssignment>> streamSupplier = () -> roleAssignments.stream()
            .filter(roleAssignment -> GrantType.SPECIFIC.name().equals(roleAssignment.getGrantType()));

        BoolQueryBuilder specificQuery =  QueryBuilders.boolQuery();
        Lists.newArrayList(createClassification(streamSupplier.get()),
            getJurisdictions(streamSupplier),
            getCaseReferences(streamSupplier))
            .stream()
            .filter(query -> query.isPresent())
            .map(query -> query.get())
            .forEach(query -> specificQuery.must(query));
        return specificQuery;
    }
}
