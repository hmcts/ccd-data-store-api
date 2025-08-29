package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class SearchParty implements Serializable, Copyable<SearchParty> {
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

    @JsonIgnore
    @Override
    public SearchParty createCopy() {
        SearchParty copy = new SearchParty();
        copy.setCaseTypeId(this.caseTypeId);
        copy.setSearchPartyDob(this.searchPartyDob);
        copy.setSearchPartyDod(this.searchPartyDod);
        copy.setSearchPartyPostCode(this.searchPartyPostCode);
        copy.setSearchPartyAddressLine1(this.searchPartyAddressLine1);
        copy.setSearchPartyEmailAddress(this.searchPartyEmailAddress);
        copy.setLiveFrom(this.liveFrom != null ? new Date(this.liveFrom.getTime()) : null);
        copy.setLiveTo(this.liveTo != null ? new Date(this.liveTo.getTime()) : null);
        copy.setSearchPartyName(this.searchPartyName);
        copy.setSearchPartyCollectionFieldName(this.searchPartyCollectionFieldName);
        return copy;
    }
}
