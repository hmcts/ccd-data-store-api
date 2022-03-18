package uk.gov.hmcts.ccd.domain.model.refdata;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;
import java.util.List;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class BuildingLocation implements Serializable {
    String buildingLocationId;
    String buildingLocationName;
    String epimmsId;
    String buildingLocationStatus;
    String area;
    String region;
    String regionId;
    String clusterName;
    String clusterId;
    String courtFinderUrl;
    String postcode;
    String address;
    List<CourtVenue> courtVenues;
}
