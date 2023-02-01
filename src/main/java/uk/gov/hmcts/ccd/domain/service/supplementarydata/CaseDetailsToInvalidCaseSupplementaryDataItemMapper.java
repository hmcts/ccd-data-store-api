package uk.gov.hmcts.ccd.domain.service.supplementarydata;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.v2.external.domain.InvalidCaseSupplementaryDataItem;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CaseDetailsToInvalidCaseSupplementaryDataItemMapper {

    public List<InvalidCaseSupplementaryDataItem> mapToDataItem(List<CaseDetails> caseDetails) {
        return caseDetails.stream().map(this::mapToDataItem).collect(Collectors.toList());
    }

    private InvalidCaseSupplementaryDataItem mapToDataItem(CaseDetails caseDetails) {
        Map<String, JsonNode> data = caseDetails.getData();
        String caseAccessCategory = JacksonUtils.getValueFromPath("CaseAccessCategory", data);
        String applicant1OrganisationPolicy = JacksonUtils.getValueFromPath(
            "applicant1OrganisationPolicy.Organisation.OrganisationID", data);
        String applicant2OrganisationPolicy = JacksonUtils.getValueFromPath(
            "applicant2OrganisationPolicy.Organisation.OrganisationID", data);
        String respondent1OrganisationPolicy = JacksonUtils.getValueFromPath(
            "respondent1OrganisationPolicy.Organisation.OrganisationID", data);
        String respondent2OrganisationPolicy = JacksonUtils.getValueFromPath(
            "respondent2OrganisationPolicy.Organisation.OrganisationID", data);

        return InvalidCaseSupplementaryDataItem.builder()
            .caseId(caseDetails.getReference())
            .caseTypeId(caseDetails.getCaseTypeId())
            .jurisdiction(caseDetails.getJurisdiction())
            .supplementaryData(caseDetails.getSupplementaryData())
            .caseAccessCategory(caseAccessCategory)
            .applicant1OrganisationPolicy(applicant1OrganisationPolicy)
            .applicant2OrganisationPolicy(applicant2OrganisationPolicy)
            .respondent1OrganisationPolicy(respondent1OrganisationPolicy)
            .respondent2OrganisationPolicy(respondent2OrganisationPolicy)
            .build();
    }
}
