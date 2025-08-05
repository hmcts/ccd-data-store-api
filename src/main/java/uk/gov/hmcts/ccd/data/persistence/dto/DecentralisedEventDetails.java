package uk.gov.hmcts.ccd.data.persistence.dto;


import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
@Builder
public class DecentralisedEventDetails {

    private String caseType;
    private String eventId;
    private String eventName;
    private String stateName;
    private String description;
    private String summary;
    private String proxiedBy;
    private String proxiedByFirstName;
    private String proxiedByLastName;
}
