package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;

import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.JURISDICTION_FIELD_KEYWORD_COL;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.LOCATION;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.REGION;

@Component
public class StandardGrantTypeESQueryBuilder implements GrantTypeESQueryBuilder {

    @Override
    public List<TermsQueryBuilder> createQuery(List<RoleAssignment> roleAssignments) {
        Supplier<Stream<RoleAssignment>> streamSupplier = () -> roleAssignments.stream()
            .filter(roleAssignment -> GrantType.STANDARD.name().equals(roleAssignment.getGrantType()))
            .filter(roleAssignment -> roleAssignment.getAuthorisations() == null
                || roleAssignment.getAuthorisations().size() == 0);
        List<TermsQueryBuilder> innerQuery = Lists.newArrayList();

        streamSupplier.get()
            .filter(roleAssignment -> roleAssignment.getAttributes() != null)
            .forEach(roleAssignment -> {
                Optional<String> jurisdiction = roleAssignment.getAttributes().getJurisdiction();

                if (jurisdiction != null
                    && StringUtils.isNotBlank(jurisdiction.orElse(""))) {
                    innerQuery.add(QueryBuilders.termsQuery(JURISDICTION_FIELD_KEYWORD_COL, jurisdiction.get()));
                }

                Optional<String> region = roleAssignment.getAttributes().getRegion();
                if (region != null
                    && StringUtils.isNotBlank(region.orElse(""))) {
                    innerQuery.add(QueryBuilders.termsQuery(REGION, region.get()));
                }

                Optional<String> location = roleAssignment.getAttributes().getLocation();
                if (location != null &&
                    StringUtils.isNotBlank(location.orElse(""))) {
                    innerQuery.add(QueryBuilders.termsQuery(LOCATION, location.get()));
                }
            });

        List<TermsQueryBuilder> standardQueries = Lists.newArrayList(createClassification(streamSupplier.get()))
            .stream()
            .filter(query -> query.isPresent())
            .map(query -> query.get())
            .collect(Collectors.toList());
        standardQueries.addAll(innerQuery);

        return standardQueries;
    }
}
