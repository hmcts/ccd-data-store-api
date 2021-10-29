package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

@Slf4j
@Component
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class ChallengedGrantTypeQueryBuilder implements GrantTypeQueryBuilder {

    private static final String QUERY = "%s in (:%s)";

    private AccessControlService accessControlService;

    @Autowired
    public ChallengedGrantTypeQueryBuilder(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @Override
    @SuppressWarnings("java:S2789")
    public String createQuery(List<RoleAssignment> roleAssignments,
                              Map<String, Object> params,
                              List<CaseStateDefinition> caseStates) {
        Supplier<Stream<RoleAssignment>> streamSupplier = () -> roleAssignments.stream()
            .filter(roleAssignment -> GrantType.CHALLENGED.name().equals(roleAssignment.getGrantType()));

        Set<String> jurisdictions = streamSupplier.get()
            .filter(roleAssignment -> roleAssignment.getAttributes() != null)
            .map(roleAssignment -> roleAssignment.getAttributes().getJurisdiction())
            .filter(jurisdictionOptional -> jurisdictionOptional != null)
            .map(jurisdictionOptional -> jurisdictionOptional.get())
            .filter(jurisdiction -> StringUtils.isNotBlank(jurisdiction))
            .collect(Collectors.toSet());

        String tmpQuery = createClassification(params, "challenged",
            streamSupplier,
            accessControlService,
            caseStates);

        if (jurisdictions.size() > 0) {
            String paramName = "jurisdictions_challenged";
            params.put(paramName, jurisdictions);
            return String.format(QUERY_WRAPPER,
                tmpQuery + getOperator(tmpQuery, AND) + String.format(QUERY, JURISDICTION, paramName));
        }

        return StringUtils.isNotBlank(tmpQuery) ? String.format(QUERY_WRAPPER, tmpQuery) : tmpQuery;
    }
}
