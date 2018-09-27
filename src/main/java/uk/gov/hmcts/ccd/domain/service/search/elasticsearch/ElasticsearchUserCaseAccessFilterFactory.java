package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.util.Optional;

import static uk.gov.hmcts.ccd.domain.service.search.filter.CaseSearchFilter.CASE_ID;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.domain.service.search.filter.CaseFilterFactory;
import uk.gov.hmcts.ccd.domain.service.search.filter.UserAccessFilter;

@Component
public class ElasticsearchUserCaseAccessFilterFactory extends UserAccessFilter implements CaseFilterFactory<QueryBuilder> {

    @Autowired
    public ElasticsearchUserCaseAccessFilterFactory(CaseAccessService caseAccessService) {
        super(caseAccessService);
    }

    @Override
    public Optional<QueryBuilder> create(String caseTypeId) {
        return getGrantedCaseIdsForRestrictedRoles().map(caseIds -> QueryBuilders.termsQuery(CASE_ID.filterName(), caseIds));
    }

}
