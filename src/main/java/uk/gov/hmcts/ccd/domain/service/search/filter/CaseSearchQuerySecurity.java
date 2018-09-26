package uk.gov.hmcts.ccd.domain.service.search.filter;

public interface CaseSearchQuerySecurity {

    String secureQuery(String caseTypeId, String query);

}
