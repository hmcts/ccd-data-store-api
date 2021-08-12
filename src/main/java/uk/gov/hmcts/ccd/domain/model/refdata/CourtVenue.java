package uk.gov.hmcts.ccd.domain.model.refdata;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class CourtVenue implements Serializable {
    String courtVenueId;
    String siteName;
    String courtName;
    String epimsId;
    String openForPublic;
    String courtTypeId;
    String courtType;
    String regionId;
    String region;
    String clusterId;
    String clusterName;
    String courtStatus;
    LocalDate courtOpenDate;
    LocalDate closedDate;
    String postcode;
    String courtAddress;
    String phoneNumber;
    String courtLocationCode;
    String dxAddress;
    String welshSiteName;
    String welshCourtAddress;
}
