package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Data
public class SearchParty implements Serializable {

    private String caseTypeId;
    private String searchPartyDob;
    private String searchPartyDod;
    private String searchPartyPostCode;
    private String searchPartyAddressLine1;
    private String searchPartyEmailAddress;
    private Date liveFrom;
    private Date liveTo;
    private String searchPartyName;
}
