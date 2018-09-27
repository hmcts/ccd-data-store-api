package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import io.searchbox.core.Search;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.service.search.filter.CaseSearchQuerySecurity;
import uk.gov.hmcts.ccd.domain.service.search.filter.CaseSearchRequestFactory;

@Component
public class ElasticsearchCaseSearchRequestFactory extends CaseSearchRequestFactory<Search> {

    @Autowired
    public ElasticsearchCaseSearchRequestFactory(ApplicationParams applicationParams, CaseSearchQuerySecurity caseSearchQuerySecurity) {
        super(applicationParams, caseSearchQuerySecurity);
    }

    @Override
    protected Search createSearchRequest(String caseTypeId, String query) {
        return new Search.Builder(query)
            .addIndex(getCaseIndexName(caseTypeId))
            .addType(getCaseIndexType())
            .build();
    }

}
