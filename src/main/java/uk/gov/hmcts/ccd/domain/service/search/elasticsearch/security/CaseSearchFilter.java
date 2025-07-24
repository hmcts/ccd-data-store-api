package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import java.util.Optional;

public interface CaseSearchFilter {

    Optional<Query> getFilter(String caseTypeId);

}
