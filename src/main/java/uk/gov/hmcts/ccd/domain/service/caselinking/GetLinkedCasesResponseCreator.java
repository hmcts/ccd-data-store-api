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
import java.util.List;

import static uk.gov.hmcts.ccd.config.JacksonUtils.getValueFromPath;

@Service
public class GetLinkedCasesResponseCreator {

    public GetLinkedCasesResponse createResponse(CaseLinkRetrievalResults caseLinkRetrievalResults,
                                                 String caseReference) {
        List<CaseLinkInfo> caseLinkInfoList = createCaseLinkInfoList(caseLinkRetrievalResults.getCaseDetails(),
                                                                     caseReference);
        return GetLinkedCasesResponse.builder()
            .linkedCases(caseLinkInfoList)
            .hasMoreRecords(caseLinkRetrievalResults.isHasMoreResults())
            .build();
    }

    private List<CaseLinkInfo> createCaseLinkInfoList(List<CaseDetails> caseDetails, String caseReference) {
        List<CaseLinkInfo> caseLinkInfoList = new ArrayList<>();
        caseDetails.forEach(
            caseDetail -> caseLinkInfoList.add(CaseLinkInfo.builder()
                .caseNameHmctsInternal(caseDetail.getData()
                    .getOrDefault("caseNameHmctsInternal", NullNode.getInstance())
                    .asText(null))
                .caseReference(String.valueOf(caseDetail.getReference()))
                .ccdCaseType(caseDetail.getCaseTypeId())
                .ccdJurisdiction(caseDetail.getJurisdiction())
                .state(caseDetail.getState())
                .linkDetails(createLinkDetailsList(caseDetail, caseReference))
                .build())
        );

        return caseLinkInfoList;
    }

    private List<CaseLinkDetails> createLinkDetailsList(CaseDetails caseDetails, String caseReference) {
        final JsonNode caseLinksJsonNode = caseDetails.getData().get(CaseLinkExtractor.STANDARD_CASE_LINK_FIELD);
        List<CaseLinkDetails> caseLinkDetailsList = new ArrayList<>();
        if (caseLinksJsonNode != null) {
            caseLinksJsonNode.forEach(caseLinkJsonNode -> {
                // extract value for item in collection
                final JsonNode valueNode = caseLinkJsonNode.get("value");
                // NB: only need to extract case link details from links to the caseReference used in the search
                if (caseReference.equals(getValueFromPath("CaseReference", valueNode))) {
                    final String createdDateTime = getValueFromPath("CreatedDateTime", valueNode);

                    List<Reason> reasonForLinks = new ArrayList<>();
                    final JsonNode reasonForLinkValue = valueNode.get("ReasonForLink");
                    if (reasonForLinkValue != null) {
                        reasonForLinkValue.forEach(reasonForLinkNode ->
                            reasonForLinks.add(Reason.builder()
                                .reasonCode(getValueFromPath("value.Reason", reasonForLinkNode))
                                .otherDescription(getValueFromPath("value.OtherDescription", reasonForLinkNode))
                                .build()));
                    }

                    CaseLinkDetails caseLinkDetails = CaseLinkDetails.builder()
                        .createdDateTime(createdDateTime != null ? LocalDateTime.parse(createdDateTime) : null)
                        .reasons(reasonForLinks)
                        .build();

                    caseLinkDetailsList.add(caseLinkDetails);
                }
            });
        }

        return caseLinkDetailsList;
    }
}
