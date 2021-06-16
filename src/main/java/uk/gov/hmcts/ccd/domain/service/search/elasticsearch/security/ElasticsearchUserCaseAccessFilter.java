package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.REFERENCE_FIELD_COL;

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
        return getGrantedCaseReferencesForRestrictedRoles().map(caseReferences -> {
            Duration between = Duration.between(start, Instant.now());
            log.info("retrieved {} granted case references in {} millisecs...",
                    caseReferences.size(), between.toMillis());
            return QueryBuilders.termsQuery(REFERENCE_FIELD_COL, caseReferences);
        });
    }

    private Optional<List<Long>> getGrantedCaseReferencesForRestrictedRoles() {
        return caseAccessService.getGrantedCaseReferencesForRestrictedRoles();
    }
}
