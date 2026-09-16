package uk.gov.hmcts.ccd.decentralised.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // For forwards compatibility with future fields
public class DecentralisedEventDetails {

    private String caseType;
    private String eventId;
    private String eventName;
    private String description;
    private String summary;
    private String proxiedBy;
    private String proxiedByFirstName;
    private String proxiedByLastName;
}
