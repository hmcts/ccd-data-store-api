package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class SearchPartyDefinition implements Serializable {

    private String caseTypeId;
    private String searchPartyDob;
    private String searchPartyDod;
    private String searchPartyPostCode;
    private String searchPartyAddressLine1;
    private String searchPartyEmailAddress;
    private Date liveFrom;
    private Date liveTo;
    private String searchPartyName;
    private String searchPartyCollectionFieldName;

}
