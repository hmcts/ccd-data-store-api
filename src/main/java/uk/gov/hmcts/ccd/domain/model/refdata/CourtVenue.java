package uk.gov.hmcts.ccd.domain.model.refdata;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;
import java.time.LocalDate;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class CourtVenue implements Serializable {
    String courtVenueId;
    String epimmsId;
    String siteName;
    String regionId;
    String region;
    String courtType;
    String courtTypeId;
    String clusterId;
    String clusterName;
    String openForPublic;
    String courtAddress;
    String postcode;
    String phoneNumber;
    LocalDate closedDate;
    String courtLocationCode;
    String dxAddress;
    String welshSiteName;
    String welshCourtAddress;
    String courtStatus;
    LocalDate courtOpenDate;
    String courtName;
}
