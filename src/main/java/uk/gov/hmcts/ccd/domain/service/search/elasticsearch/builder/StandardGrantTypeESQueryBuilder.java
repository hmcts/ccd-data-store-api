package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.JURISDICTION_FIELD_KEYWORD_COL;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.LOCATION;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.REGION;

@Component
public class StandardGrantTypeESQueryBuilder implements GrantTypeESQueryBuilder {

    private AccessControlService accessControlService;

    public StandardGrantTypeESQueryBuilder(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @Override
    @SuppressWarnings("java:S2789")
    public BoolQueryBuilder createQuery(List<RoleAssignment> roleAssignments,
                                        List<CaseStateDefinition> caseStates,
                                        Set<AccessProfile> accessProfiles) {
        Supplier<Stream<RoleAssignment>> streamSupplier = () -> roleAssignments.stream()
            .filter(roleAssignment -> GrantType.STANDARD.name().equals(roleAssignment.getGrantType()));

        BoolQueryBuilder standardInnerQuery = QueryBuilders.boolQuery();

        streamSupplier.get()
            .filter(roleAssignment -> roleAssignment.getAttributes() != null)
            .map(roleAssignment -> {
                BoolQueryBuilder innerQuery = QueryBuilders.boolQuery();
                Optional<String> jurisdiction = roleAssignment.getAttributes().getJurisdiction();

                if (jurisdiction != null
                    && StringUtils.isNotBlank(jurisdiction.orElse(""))) {
                    innerQuery.must(QueryBuilders.matchQuery(JURISDICTION_FIELD_KEYWORD_COL, jurisdiction.get()));
                }

                Optional<String> region = roleAssignment.getAttributes().getRegion();
                if (region != null
                    && StringUtils.isNotBlank(region.orElse(""))) {
                    innerQuery.must(QueryBuilders.matchQuery(REGION, region.get()));
                }

                Optional<String> location = roleAssignment.getAttributes().getLocation();
                if (location != null
                    && StringUtils.isNotBlank(location.orElse(""))) {
                    innerQuery.must(QueryBuilders.matchQuery(LOCATION, location.get()));
                }
                return innerQuery;
            }).forEach(innerQuery -> standardInnerQuery.should(innerQuery));

        Optional<TermsQueryBuilder> classificationTermsQuery = createClassification(streamSupplier.get());
        BoolQueryBuilder standardQuery = QueryBuilders.boolQuery();

        if (classificationTermsQuery.isPresent() && standardInnerQuery.hasClauses()) {
            standardQuery.must(classificationTermsQuery.get());
            standardQuery.must(standardInnerQuery);
        } else if (standardInnerQuery.hasClauses()) {
            standardQuery.must(standardInnerQuery);
        } else if (classificationTermsQuery.isPresent()) {
            standardQuery.must(classificationTermsQuery.get());
        }

        Lists.newArrayList(createCaseStateQuery(streamSupplier, accessControlService, caseStates, accessProfiles))
            .stream()
            .filter(query -> query.isPresent())
            .map(query -> query.get())
            .forEach(query -> standardQuery.must(query));

        return standardQuery;
    }
}
