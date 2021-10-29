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
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

@Slf4j
@Component
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class ExcludedGrantTypeQueryBuilder implements GrantTypeQueryBuilder {

    private static final String QUERY = "%s in (:%s)";

    private AccessControlService accessControlService;

    @Autowired
    public ExcludedGrantTypeQueryBuilder(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @Override
    @SuppressWarnings("java:S2789")
    public String createQuery(List<RoleAssignment> roleAssignments,
                              Map<String, Object> params,
                              List<CaseStateDefinition> caseStates,
                              Set<AccessProfile> accessProfiles) {
        Supplier<Stream<RoleAssignment>> streamSupplier = () -> roleAssignments.stream()
            .filter(roleAssignment -> GrantType.EXCLUDED.name().equals(roleAssignment.getGrantType()));

        String tmpQuery = createClassification(params, "excluded",
            streamSupplier,
            accessControlService,
            caseStates,
            accessProfiles);


        Set<String> caseReferences = streamSupplier.get()
            .filter(roleAssignment -> roleAssignment.getAttributes() != null)
            .map(roleAssignment -> roleAssignment.getAttributes().getCaseId())
            .filter(caseIdOptional -> caseIdOptional != null)
            .map(caseIdOptional -> caseIdOptional.get())
            .filter(caseId -> StringUtils.isNotBlank(caseId))
            .collect(Collectors.toSet());

        if (caseReferences.size() > 0) {
            String paramName = "case_ids_excluded";
            params.put(paramName, caseReferences);
            tmpQuery = tmpQuery + getOperator(tmpQuery, AND) + String.format(QUERY, REFERENCE, paramName);
        }

        return StringUtils.isNotBlank(tmpQuery) ? String.format(QUERY_WRAPPER, tmpQuery) : tmpQuery;
    }
}
