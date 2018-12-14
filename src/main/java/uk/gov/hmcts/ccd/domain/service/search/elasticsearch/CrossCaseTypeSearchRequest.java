package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CrossCaseTypeSearchRequest {

    private final List<CaseSearchRequest> caseSearchRequests = new ArrayList<>();

    public void addRequest(CaseSearchRequest request) {
        caseSearchRequests.add(request);
    }

    public List<CaseSearchRequest> getCaseSearchRequests() {
        return Collections.unmodifiableList(caseSearchRequests);
    }

    public boolean isMultiCaseTypeSearch() {
        return caseSearchRequests.size() > 1;
    }
}
