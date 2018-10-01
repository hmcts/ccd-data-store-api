package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.util.Optional;

import org.elasticsearch.index.query.QueryBuilder;

public interface CaseSearchFilter {

    Optional<QueryBuilder> getFilter(String caseTypeId);

}
