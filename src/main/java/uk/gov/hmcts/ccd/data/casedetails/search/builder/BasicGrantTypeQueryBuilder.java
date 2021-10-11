package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;

@Slf4j
@Component
public class BasicGrantTypeQueryBuilder implements GrantTypeQueryBuilder {

    @Override
    public String createQuery(List<RoleAssignment> roleAssignments, Map<String, Object> params) {
        Stream<RoleAssignment> roleAssignmentStream = roleAssignments.stream()
            .filter(roleAssignment -> GrantType.BASIC.name().equals(roleAssignment.getGrantType()))
            .filter(roleAssignment -> roleAssignment.getAuthorisations() == null
                || roleAssignment.getAuthorisations().size() == 0);
        String query = createClassification(params, "classifications", roleAssignmentStream);
        if (StringUtils.isNotBlank(query)) {
            return String.format(QUERY_WRAPPER, query);
        }
        return query;
    }
}
