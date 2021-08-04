package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;

@Slf4j
@Component
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class ExcludedGrantTypeQueryBuilder implements GrantTypeQueryBuilder {

    private static final String QUERY = "%s in (:%s)";

    @Override
    public String createQuery(List<RoleAssignment> roleAssignments, Map<String, Object> params) {
        Supplier<Stream<RoleAssignment>> streamSupplier = () -> roleAssignments.stream()
            .filter(roleAssignment -> GrantType.EXCLUDED.name().equals(roleAssignment.getGrantType()));

        String tmpQuery = createClassification(params, "classifications_excluded", streamSupplier.get());

        Set<String> caseReferences = streamSupplier.get()
            .map(roleAssignment -> roleAssignment.getAttributes().getCaseId())
            .flatMap(Optional::stream)
            .collect(Collectors.toSet());

        if (caseReferences.size() > 0) {
            String paramName = "case_ids_excluded";
            params.put(paramName, caseReferences);
            tmpQuery = tmpQuery + getOperator(tmpQuery, AND) + String.format(QUERY, REFERENCE, paramName);
        }

        return StringUtils.isNotBlank(tmpQuery) ? String.format(QUERY_WRAPPER, tmpQuery) : tmpQuery;
    }
}
