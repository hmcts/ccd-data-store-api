package uk.gov.hmcts.ccd.v2.external.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Builder
@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class InvalidCaseSupplementaryDataItem {

    @JsonProperty("case_id")
    private Long caseId;

    @JsonProperty("supplementary_data")
    private Map<String, JsonNode> supplementaryData;

    @JsonProperty("jurisdiction")
    private String jurisdiction;

    @JsonProperty("case_type_id")
    private String caseTypeId;

    // data ->> 'CaseAccessCategory'
    @JsonProperty("case_access_category")
    private String caseAccessCategory;

    @JsonProperty("organisation_policy_organisation_ids")
    private List<String> organisationPolicyOrgIds;

    @JsonProperty("organisation_policy_case_roles")
    private List<String> orgPolicyCaseAssignedRoles;

    // We just need to confirm if there are any users assigned the case roles
    @JsonProperty("ras_user_id")
    private String userId;

    @JsonProperty("ras_case_role")
    private String caseRole;
}
