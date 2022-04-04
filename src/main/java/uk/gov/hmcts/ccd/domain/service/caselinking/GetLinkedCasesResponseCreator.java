package uk.gov.hmcts.ccd.domain.service.caselinking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLinkDetails;
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLinkInfo;
import uk.gov.hmcts.ccd.domain.model.caselinking.GetLinkedCasesResponse;
import uk.gov.hmcts.ccd.domain.model.caselinking.Reason;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class GetLinkedCasesResponseCreator {

    public GetLinkedCasesResponse createResponse(CaseLinkRetrievalResults caseLinkRetrievalResults) {
        List<CaseLinkInfo> caseLinkInfos = createCaseLinkInfos(caseLinkRetrievalResults.getCaseDetails());
        return GetLinkedCasesResponse.builder()
            .linkedCases(caseLinkInfos)
            .hasMoreRecords(caseLinkRetrievalResults.isHasMoreResults())
            .build();
    }

    private List<CaseLinkInfo> createCaseLinkInfos(List<CaseDetails> caseDetails) {
        List<CaseLinkInfo> caseLinkInfos = new ArrayList<>();
        caseDetails.forEach(
            caseDetail -> caseLinkInfos.add(CaseLinkInfo.builder()
                .caseNameHmctsInternal(caseDetail.getData()
                    .getOrDefault("caseNameHmctsInternal", NullNode.getInstance())
                    .asText(null))
                .caseReference(String.valueOf(caseDetail.getReference()))
                .ccdCaseType(caseDetail.getCaseTypeId())
                .ccdJurisdiction(caseDetail.getJurisdiction())
                .state(caseDetail.getState())
                .linkDetails(createCaseDetails(caseDetail))
                .build())
        );

        return caseLinkInfos;
    }

    private List<CaseLinkDetails> createCaseDetails(CaseDetails caseDetails) {
        final JsonNode caseLinkJsonNode = caseDetails.getData().get("CaseLink");
        if (caseLinkJsonNode != null) {
            final String createdDateTime = caseLinkJsonNode.findValue("CreatedDateTime").asText();
            final JsonNode reasonForLinkValue = caseLinkJsonNode.get("ReasonForLink");
            List<Reason> reasonForLinks = new ArrayList<>();
            reasonForLinkValue.forEach(reasonForLinkNode ->
                reasonForLinks.add(Reason.builder()
                    .reasonCode(reasonForLinkNode.get("value").get("Reason").asText())
                    .otherDescription(reasonForLinkNode.get("value").get("OtherDescription").asText())
                    .build()));
            CaseLinkDetails caseLinkDetails = CaseLinkDetails.builder()
                .createdDateTime(LocalDateTime.parse(createdDateTime))
                .reasons(reasonForLinks)
                .build();
            return List.of(caseLinkDetails);
        }
        return Collections.emptyList();
    }
}
