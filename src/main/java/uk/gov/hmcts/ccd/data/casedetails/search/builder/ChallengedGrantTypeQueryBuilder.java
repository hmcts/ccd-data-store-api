package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private static final String QUERY = "%s in (:%s)";

    @Override
    public String createQuery(List<RoleAssignment> roleAssignments, Map<String, Object> params) {
        Supplier<Stream<RoleAssignment>> streamSupplier = () -> roleAssignments.stream()
            .filter(roleAssignment -> GrantType.CHALLENGED.name().equals(roleAssignment.getGrantType()));

        Set<String> jurisdictions = streamSupplier.get()
            .map(roleAssignment -> roleAssignment.getAttributes().getJurisdiction())
            .flatMap(Optional::stream)
            .collect(Collectors.toSet());

        String tmpQuery = createClassification(params, "classifications_challenged", streamSupplier.get());;

        if (jurisdictions.size() > 0) {
            String paramName = "jurisdictions_challenged";
            params.put(paramName, jurisdictions);
            return String.format(QUERY_WRAPPER,
                tmpQuery + getOperator(tmpQuery, AND) + String.format(QUERY, JURISDICTION, paramName));
        }

        return EMPTY;
    }
}
