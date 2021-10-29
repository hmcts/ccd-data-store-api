package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

@Slf4j
@Component
public class BasicGrantTypeQueryBuilder implements GrantTypeQueryBuilder {

    private AccessControlService accessControlService;

    public BasicGrantTypeQueryBuilder(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @Override
    public String createQuery(List<RoleAssignment> roleAssignments,
                              Map<String, Object> params,
                              List<CaseStateDefinition> caseStates) {
        Supplier<Stream<RoleAssignment>> roleAssignmentStream = () -> roleAssignments.stream()
            .filter(roleAssignment -> GrantType.BASIC.name().equals(roleAssignment.getGrantType()));
        String query = createClassification(params, "basic",
            roleAssignmentStream, accessControlService, caseStates);
        if (StringUtils.isNotBlank(query)) {
            return String.format(QUERY_WRAPPER, query);
        }
        return query;
    }
}
