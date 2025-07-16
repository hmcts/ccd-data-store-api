package uk.gov.hmcts.ccd.domain.model.lau;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionLogPostResponse  {

    private String caseActionId;
    private String userId;
    private String caseRef;
    private String caseJurisdictionId;
    private String caseTypeId;
    private String caseAction;
    private String timestamp;
}
