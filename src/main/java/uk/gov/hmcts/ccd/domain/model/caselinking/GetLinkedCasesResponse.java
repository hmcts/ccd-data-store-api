package uk.gov.hmcts.ccd.domain.model.caselinking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetLinkedCasesResponse {
    private boolean hasMoreRecords;
    private List<CaseLinkInfo> linkedCases;
}
