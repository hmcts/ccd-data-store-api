package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import java.util.List;
import java.util.stream.Stream;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;

@Component
public class BasicGrantTypeESQueryBuilder implements GrantTypeESQueryBuilder {

    @Override
    public BoolQueryBuilder createQuery(List<RoleAssignment> roleAssignments) {
        Stream<RoleAssignment> roleAssignmentStream = roleAssignments.stream()
            .filter(roleAssignment -> GrantType.BASIC.name().equals(roleAssignment.getGrantType()))
            .filter(roleAssignment -> roleAssignment.getAuthorisations() == null
                || roleAssignment.getAuthorisations().size() == 0);
        return createClassification(roleAssignmentStream);
    }
}
