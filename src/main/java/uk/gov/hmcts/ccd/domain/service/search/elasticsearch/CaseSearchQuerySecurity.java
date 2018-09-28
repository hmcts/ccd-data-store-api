package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

public interface CaseSearchQuerySecurity {

    String secureQuery(String caseTypeId, String query);

}
