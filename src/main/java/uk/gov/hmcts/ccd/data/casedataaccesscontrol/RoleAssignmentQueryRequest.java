package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentAttributes;

import java.util.List;
import java.util.Optional;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor

public class RoleAssignmentQueryRequest {


    private Optional<String> actorId;
    private Optional<String> roleType;
    private Optional<String> roleName;
    private Optional<String> classification;
    private Optional<String> grantType;
    private Optional<String> roleCategory;
    private Optional<String> validAt;
    private Optional<List<String>> authorisations;
    private Optional<RoleAssignmentAttributes> attributes;
    private MultiValueMap<String, Optional<String>> attributesMap = new LinkedMultiValueMap<>();
    private MultiValueMap<String, Object> queryParamsMap = new LinkedMultiValueMap<>();


    public RoleAssignmentQueryRequest(Optional<String> actorId, Optional<String> roleType, Optional<String> roleName,
                                      Optional<String> classification, Optional<String> grantType,
                                      Optional<String> roleCategory, Optional<String> validAt,
                                      Optional<List<String>> authorisations,
                                      Optional<RoleAssignmentAttributesResource> attributes) {

        attributesMap.add("jurisdiction", attributes.flatMap(RoleAssignmentAttributesResource::getJurisdiction));
        attributesMap.add("caseType", attributes.flatMap(RoleAssignmentAttributesResource::getCaseType));
        attributesMap.add("caseId", attributes.flatMap(RoleAssignmentAttributesResource::getCaseId));
        attributesMap.add("region", attributes.flatMap(RoleAssignmentAttributesResource::getRegion));
        attributesMap.add("location", attributes.flatMap(RoleAssignmentAttributesResource::getLocation));
        attributesMap.add("contractType", attributes.flatMap(RoleAssignmentAttributesResource::getContractType));

        queryParamsMap.add("actorId", actorId);
        queryParamsMap.add("roleType", roleType);
        queryParamsMap.add("roleName", roleName);
        queryParamsMap.add("classification", classification);
        queryParamsMap.add("grantType", grantType);
        queryParamsMap.add("roleCategory", roleCategory);
        queryParamsMap.add("grantType", grantType);
        queryParamsMap.add("validAt", validAt);
        queryParamsMap.add("authorisations", authorisations);
        queryParamsMap.add("attributes", attributesMap);

    }
}

