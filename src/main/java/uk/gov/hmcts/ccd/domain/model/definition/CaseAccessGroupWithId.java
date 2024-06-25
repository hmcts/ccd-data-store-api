package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
//So that the newly created CaseAccessGroup can
//be authorised and seen in the UI
@JsonRootName(value = "value")
public class CaseAccessGroupWithId {
    @JsonProperty("value")
    CaseAccessGroup caseAccessGroup;
    String id;
}
