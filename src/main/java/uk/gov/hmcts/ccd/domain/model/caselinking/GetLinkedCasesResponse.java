package uk.gov.hmcts.ccd.domain.model.caselinking;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GetLinkedCasesResponse {
    private boolean hasMoreRecords;
    private List<CaseLinkInfo> linkedCases;
}
