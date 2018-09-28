package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import static java.lang.String.format;

import uk.gov.hmcts.ccd.ApplicationParams;

public abstract class CaseSearchRequestFactory<T> {

    private final ApplicationParams applicationParams;
    private final CaseSearchQuerySecurity caseSearchQuerySecurity;

    protected CaseSearchRequestFactory(ApplicationParams applicationParams,
                                       CaseSearchQuerySecurity caseSearchQuerySecurity) {
        this.applicationParams = applicationParams;
        this.caseSearchQuerySecurity = caseSearchQuerySecurity;
    }

    public final T create(String caseTypeId, String query) {
        String securedQuery = caseSearchQuerySecurity.secureQuery(caseTypeId, query);
        return createSearchRequest(caseTypeId, securedQuery);
    }

    protected abstract T createSearchRequest(String caseTypeId, String query);

    protected String getCaseIndexName(String caseTypeId) {
        return format(applicationParams.getCasesIndexNameFormat(), caseTypeId.toLowerCase());
    }

    protected String getCaseIndexType() {
        return applicationParams.getCasesIndexType();
    }
}
