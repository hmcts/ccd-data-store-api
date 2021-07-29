package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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

        String regionAndLocationQuery = streamSupplier.get()
            .map(roleAssignment -> {
                Optional<String> jurisdiction = roleAssignment.getAttributes().getJurisdiction();
                String innerQuery = "";
                if (jurisdiction.isPresent()) {
                    innerQuery = JURISDICTION + "=" + jurisdiction.get();
                }

                Optional<String> region = roleAssignment.getAttributes().getRegion();
                if (region.isPresent()) {
                    innerQuery = innerQuery + getOperator(innerQuery, " AND ") + REGION + "=" + region.get();
                }

                Optional<String> location = roleAssignment.getAttributes().getLocation();
                if (location.isPresent()) {
                    innerQuery = innerQuery + getOperator(innerQuery, " AND ") + REGION + "=" + region.get();
                }
                return StringUtils.isNotBlank(innerQuery) ? String.format(QUERY_WRAPPER, innerQuery) : innerQuery;
            }).collect(Collectors.joining(" OR "));

        String tmpQuery = createClassification(params, streamSupplier.get());

        if (StringUtils.isNotBlank(regionAndLocationQuery)) {
            tmpQuery = tmpQuery + getOperator(tmpQuery, " AND ")
                + String.format(QUERY_WRAPPER, regionAndLocationQuery);
        }

        return StringUtils.isNotBlank(tmpQuery) ? String.format(QUERY_WRAPPER, tmpQuery) : tmpQuery;
    }
}
