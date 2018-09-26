package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.util.Optional;

import static uk.gov.hmcts.ccd.domain.service.search.filter.CaseSearchFilter.CASE_STATE;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.service.common.AuthorisedCaseDefinitionDataService;
import uk.gov.hmcts.ccd.domain.service.search.filter.CaseFilterFactory;
import uk.gov.hmcts.ccd.domain.service.search.filter.CaseStateFilter;

@Component
public class ElasticsearchCaseStateFilterFactory extends CaseStateFilter implements CaseFilterFactory<QueryBuilder> {

    @Autowired
    public ElasticsearchCaseStateFilterFactory(
        AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService) {
        super(authorisedCaseDefinitionDataService);
    }

    @Override
    public Optional<QueryBuilder> create(String caseTypeId) {
        return Optional.of(QueryBuilders.termsQuery(CASE_STATE.filterName(), getCaseStateIdsForUserReadAccess(caseTypeId)));
    }

}
