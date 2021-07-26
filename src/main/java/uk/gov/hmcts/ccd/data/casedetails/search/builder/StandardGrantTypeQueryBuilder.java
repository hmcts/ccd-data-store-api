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
public class StandardGrantTypeQueryBuilder implements GrantTypeQueryBuilder {

    @Override
    public String createQuery(List<RoleAssignment> roleAssignments, Map<String, Object> params) {
        Supplier<Stream<RoleAssignment>> streamSupplier = () -> roleAssignments.stream()
            .filter(roleAssignment -> GrantType.STANDARD.name().equals(roleAssignment.getGrantType()))
            .filter(roleAssignment -> roleAssignment.getAuthorisations() == null
                || roleAssignment.getAuthorisations().size() == 0);

        String classificationQuery = createClassification(params, streamSupplier.get());

        Set<String> jurisdictions = streamSupplier.get()
            .map(roleAssignment -> roleAssignment.getAttributes().getJurisdiction().orElse(""))
            .filter(jurisdiction -> jurisdiction.length() > 0)
            .collect(Collectors.toSet());

        String tmpQuery = classificationQuery;

        if (jurisdictions.size() > 0) {
            params.put("jurisdictions", jurisdictions);
            tmpQuery = tmpQuery + getOperator(tmpQuery," AND ") + JURISDICTION + " in (:jurisdictions)";
        }

        Set<String> regions = streamSupplier.get()
            .map(roleAssignment -> roleAssignment.getAttributes().getRegion().orElse(""))
            .filter(region -> region.length() > 0)
            .collect(Collectors.toSet());

        if (regions.size() > 0) {
            params.put("regions", regions);
            tmpQuery = tmpQuery + getOperator(tmpQuery," AND ") + REGION + " in (:regions)";
        }

        Set<String> locations = streamSupplier.get()
            .map(roleAssignment -> roleAssignment.getAttributes().getLocation().orElse(""))
            .filter(location -> location.length() > 0)
            .collect(Collectors.toSet());

        if (locations.size() > 0) {
            params.put("locations", locations);
            tmpQuery = tmpQuery + getOperator(tmpQuery," AND ") + LOCATION + " in (:locations)";
        }

        return StringUtils.isNotBlank(tmpQuery) ? String.format(QUERY_WRAPPER, tmpQuery) : tmpQuery;
    }
}
