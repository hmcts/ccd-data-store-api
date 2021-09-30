package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;

@Slf4j
@Component
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class ExcludedGrantTypeQueryBuilder implements GrantTypeQueryBuilder {

    private static final String QUERY = "%s in (:%s)";

    @Override
    @SuppressWarnings("java:S2789")
    public String createQuery(List<RoleAssignment> roleAssignments, Map<String, Object> params) {
        Supplier<Stream<RoleAssignment>> streamSupplier = () -> roleAssignments.stream()
            .filter(roleAssignment -> GrantType.EXCLUDED.name().equals(roleAssignment.getGrantType()));

        String tmpQuery = createClassification(params, "classifications_excluded", streamSupplier.get());
        log.debug("[classifications_excluded] : " + params.get("classifications_excluded"));


        Set<String> caseReferences = streamSupplier.get()
            .filter(roleAssignment -> roleAssignment.getAttributes() != null)
            .map(roleAssignment -> roleAssignment.getAttributes().getCaseId())
            .filter(caseIdOptional -> caseIdOptional != null)
            .map(caseIdOptional -> caseIdOptional.get())
            .filter(caseId -> StringUtils.isNotBlank(caseId))
            .collect(Collectors.toSet());
        log.debug("[case_ids_excluded] : " + caseReferences);

        if (caseReferences.size() > 0) {
            String paramName = "case_ids_excluded";
            params.put(paramName, caseReferences);
            tmpQuery = tmpQuery + getOperator(tmpQuery, AND) + String.format(QUERY, REFERENCE, paramName);
        }

        return StringUtils.isNotBlank(tmpQuery) ? String.format(QUERY_WRAPPER, tmpQuery) : tmpQuery;
    }
}
