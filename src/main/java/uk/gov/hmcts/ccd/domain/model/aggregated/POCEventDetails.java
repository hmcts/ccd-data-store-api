package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class POCEventDetails {

    @JsonProperty("case_type")
    private String caseType;
    @JsonProperty("event_id")
    private String eventId;
    @JsonProperty("event_name")
    private String eventName;
    @JsonProperty("state_name")
    private String stateName;
    private String description;
    private String summary;
    @JsonProperty("proxied_by")
    private String proxiedBy;
    @JsonProperty("proxied_by_first_name")
    private String proxiedByFirstName;
    @JsonProperty("proxied_by_last_name")
    private String proxiedByLastName;
}
