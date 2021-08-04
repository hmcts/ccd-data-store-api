package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;

@Component
public class ChallengedGrantTypeESQueryBuilder implements GrantTypeESQueryBuilder {

    @Override
    public BoolQueryBuilder createQuery(List<RoleAssignment> roleAssignments) {
        Supplier<Stream<RoleAssignment>> streamSupplier = () -> roleAssignments.stream()
            .filter(roleAssignment -> GrantType.CHALLENGED.name().equals(roleAssignment.getGrantType()));

        BoolQueryBuilder boolQueryBuilder = createClassification(streamSupplier.get());
        addJurisdictions(streamSupplier, boolQueryBuilder);
        return boolQueryBuilder;
    }
}
