package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;

import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.JURISDICTION_FIELD_COL;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.LOCATION;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.REGION;

@Component
public class StandardGrantTypeESQueryBuilder implements GrantTypeESQueryBuilder {

    @Override
    public BoolQueryBuilder createQuery(List<RoleAssignment> roleAssignments) {
        Supplier<Stream<RoleAssignment>> streamSupplier = () -> roleAssignments.stream()
            .filter(roleAssignment -> GrantType.STANDARD.name().equals(roleAssignment.getGrantType()))
            .filter(roleAssignment -> roleAssignment.getAuthorisations() == null
                || roleAssignment.getAuthorisations().size() == 0);

        BoolQueryBuilder boolQueryBuilder = createClassification(streamSupplier.get());

        streamSupplier.get()
            .forEach(roleAssignment -> {
                Optional<String> jurisdiction = roleAssignment.getAttributes().getJurisdiction();
                BoolQueryBuilder innerQuery = QueryBuilders.boolQuery();

                if (jurisdiction.isPresent()) {
                    innerQuery.must(QueryBuilders.matchQuery(JURISDICTION_FIELD_COL, jurisdiction));
                }

                Optional<String> region = roleAssignment.getAttributes().getRegion();
                if (region.isPresent()) {
                    innerQuery.must(QueryBuilders.matchQuery(REGION, region));
                }

                Optional<String> location = roleAssignment.getAttributes().getLocation();
                if (location.isPresent()) {
                    innerQuery.must(QueryBuilders.matchQuery(LOCATION, location));
                }
                boolQueryBuilder.should(innerQuery);
            });

        addJurisdictions(streamSupplier, boolQueryBuilder);

        Set<String> regions = streamSupplier.get()
            .map(roleAssignment -> roleAssignment.getAttributes().getRegion().orElse(""))
            .filter(region -> region.length() > 0)
            .collect(Collectors.toSet());

        if (regions.size() > 0) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(REGION, regions));
        }

        Set<String> locations = streamSupplier.get()
            .map(roleAssignment -> roleAssignment.getAttributes().getLocation().orElse(""))
            .filter(location -> location.length() > 0)
            .collect(Collectors.toSet());

        if (locations.size() > 0) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(LOCATION, regions));
        }

        return boolQueryBuilder;
    }
}
