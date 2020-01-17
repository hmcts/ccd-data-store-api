package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.ID_FIELD_COL;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;

@Component
@Slf4j
public class ElasticsearchUserCaseAccessFilter implements CaseSearchFilter {

    private final CaseAccessService caseAccessService;

    @Autowired
    public ElasticsearchUserCaseAccessFilter(CaseAccessService caseAccessService) {
        this.caseAccessService = caseAccessService;
    }

    @Override
    public Optional<QueryBuilder> getFilter(String caseTypeId) {
        Instant start = Instant.now();
        return getGrantedCaseIdsForRestrictedRoles().map(caseIds -> {
            Duration between = Duration.between(start, Instant.now());
            log.info("retrieved {} granted case ids in {} millisecs...", caseIds.size(), between.toMillis());
            return QueryBuilders.termsQuery(ID_FIELD_COL, caseIds);
        });
    }

    private Optional<List<Long>> getGrantedCaseIdsForRestrictedRoles() {
        return caseAccessService.getGrantedCaseIdsForRestrictedRoles();
    }
}
