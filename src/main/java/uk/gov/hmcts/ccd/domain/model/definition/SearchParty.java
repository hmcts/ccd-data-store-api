package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.io.Serializable;
import java.util.Date;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Value
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PACKAGE)
public class SearchParty implements Serializable {

    String caseTypeId;
    String searchPartyDob;
    String searchPartyDod;
    String searchPartyPostCode;
    String searchPartyAddressLine1;
    String searchPartyEmailAddress;
    Date liveFrom;
    Date liveTo;
    String searchPartyName;
    String searchPartyCollectionFieldName;
}
