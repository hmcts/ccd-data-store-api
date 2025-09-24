package uk.gov.hmcts.ccd.data.persistence.dto;


import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
@Builder
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
    private LocalDate resolvedTtl;
}
