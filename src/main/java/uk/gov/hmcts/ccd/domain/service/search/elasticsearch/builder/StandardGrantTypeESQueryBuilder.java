package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
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

                if (StringUtils.isNotBlank(jurisdiction.orElse(""))) {
                    innerQuery.must(QueryBuilders.termQuery(JURISDICTION_FIELD_COL, jurisdiction.get()));
                }

                Optional<String> region = roleAssignment.getAttributes().getRegion();
                if (StringUtils.isNotBlank(region.orElse(""))) {
                    innerQuery.must(QueryBuilders.termQuery(REGION, region.get()));
                }

                Optional<String> location = roleAssignment.getAttributes().getLocation();
                if (StringUtils.isNotBlank(location.orElse(""))) {
                    innerQuery.must(QueryBuilders.termQuery(LOCATION, location.get()));
                }
                boolQueryBuilder.should(innerQuery);
            });

        return boolQueryBuilder;
    }
}
