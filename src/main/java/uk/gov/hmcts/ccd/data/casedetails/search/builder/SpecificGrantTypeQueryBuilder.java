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
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;

@Slf4j
@Component
public class SpecificGrantTypeQueryBuilder implements GrantTypeQueryBuilder {

    @Override
    public String createQuery(List<RoleAssignment> roleAssignments, Map<String, Object> params) {
        Supplier<Stream<RoleAssignment>> streamSupplier = () -> roleAssignments.stream()
            .filter(roleAssignment -> GrantType.SPECIFIC.name().equals(roleAssignment.getGrantType()))
            .filter(roleAssignment -> roleAssignment.getAuthorisations() == null
                || roleAssignment.getAuthorisations().size() == 0);

        String classficationQuery = createClassification(params, streamSupplier.get());

        String tmpQuery = classficationQuery;

        Set<String> jurisdictions = streamSupplier.get()
            .map(roleAssignment -> roleAssignment.getAttributes().getJurisdiction().orElse(""))
            .filter(jurisdiction -> jurisdiction.length() > 0)
            .collect(Collectors.toSet());

        if (jurisdictions.size() > 0) {
            params.put("jurisdictions", jurisdictions);
            tmpQuery = tmpQuery +  getOperator(tmpQuery, " AND ") + JURISDICTION + " in (:jurisdictions)";
        }

        Set<String> caseIds = streamSupplier.get()
            .map(roleAssignment -> roleAssignment.getAttributes().getCaseId().orElse(""))
            .filter(caseId -> caseId.length() > 0)
            .collect(Collectors.toSet());

        if (caseIds.size() > 0) {
            params.put("case_ids", caseIds);
            tmpQuery = tmpQuery +  getOperator(tmpQuery, " AND ") + CASE_ID + " in (:case_ids)";
        }

        return StringUtils.isNotBlank(tmpQuery) ? String.format(QUERY_WRAPPER, tmpQuery) : tmpQuery;
    }
}
