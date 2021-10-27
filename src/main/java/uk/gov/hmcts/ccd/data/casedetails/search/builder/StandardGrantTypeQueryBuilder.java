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
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentAttributes;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;

@Slf4j
@Component
public class StandardGrantTypeQueryBuilder implements GrantTypeQueryBuilder {

    @Override
    @SuppressWarnings("java:S2789")
    public String createQuery(List<RoleAssignment> roleAssignments, Map<String, Object> params) {
        Supplier<Stream<RoleAssignment>> streamSupplier = () -> roleAssignments.stream()
            .filter(roleAssignment -> GrantType.STANDARD.name().equals(roleAssignment.getGrantType()));

        String regionAndLocationQuery = streamSupplier.get()
            .filter(roleAssignment -> roleAssignment.getAttributes() != null)
            .filter(roleAssignment -> isValidRoleAssignment(roleAssignment.getAttributes()))
            .map(roleAssignment -> {
                Optional<String> jurisdiction = roleAssignment.getAttributes().getJurisdiction();
                String innerQuery = "";
                if (jurisdiction != null && jurisdiction.isPresent()) {
                    innerQuery = String.format("%s='%s'", JURISDICTION, jurisdiction.get());
                }

                Optional<String> region = roleAssignment.getAttributes().getRegion();
                if (region != null && region.isPresent()) {
                    innerQuery = innerQuery + getOperator(innerQuery, AND)
                        + String.format("%s='%s'", REGION, region.get());
                }

                Optional<String> location = roleAssignment.getAttributes().getLocation();
                if (location != null && location.isPresent()) {
                    innerQuery = innerQuery + getOperator(innerQuery, AND)
                        + String.format("%s='%s'", LOCATION, location.get());
                }
                return StringUtils.isNotBlank(innerQuery) ? String.format(QUERY_WRAPPER, innerQuery) : innerQuery;
            }).collect(Collectors.joining(" OR "));

        String tmpQuery = createClassification(params, "classifications_standard", streamSupplier.get());

        if (StringUtils.isNotBlank(regionAndLocationQuery)) {
            tmpQuery = tmpQuery + getOperator(tmpQuery, AND)
                + String.format(QUERY_WRAPPER, regionAndLocationQuery);
        }

        return StringUtils.isNotBlank(tmpQuery) ? String.format(QUERY_WRAPPER, tmpQuery) : tmpQuery;
    }

    @SuppressWarnings("java:S2789")
    private boolean isValidRoleAssignment(RoleAssignmentAttributes attributes) {
        Optional<String> jurisdiction = attributes.getJurisdiction();
        boolean isValid = jurisdiction != null && jurisdiction.isPresent();
        Optional<String> location = attributes.getLocation();
        isValid = isValid || (location != null && location.isPresent());
        Optional<String> region = attributes.getLocation();
        return isValid || (region != null && region.isPresent());
    }
}
