package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;

@Slf4j
@Component
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class ChallengedGrantTypeQueryBuilder implements GrantTypeQueryBuilder {

    private static final String QUERY = "%s in (:jurisdictions)";

    @Override
    public String createQuery(List<RoleAssignment> roleAssignments, Map<String, Object> params) {
        Supplier<Stream<RoleAssignment>> streamSupplier = () -> roleAssignments.stream()
            .filter(roleAssignment -> GrantType.CHALLENGED.name().equals(roleAssignment.getGrantType()));

        Set<String> jurisdictions = streamSupplier.get()
            .map(roleAssignment -> roleAssignment.getAttributes().getJurisdiction().orElse(""))
            .filter(jurisdiction -> jurisdiction.length() > 0)
            .collect(Collectors.toSet());

        String classificationQuery = createClassification(params, streamSupplier.get());

        String tmpQuery = classificationQuery;

        if (jurisdictions.size() > 0) {
            params.put("jurisdictions", jurisdictions);
            return String.format(QUERY_WRAPPER,
                tmpQuery + getOperator(tmpQuery, " AND ") + String.format(QUERY, JURISDICTION));
        }

        return EMPTY;
    }
}
