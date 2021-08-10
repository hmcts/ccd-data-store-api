package uk.gov.hmcts.ccd.domain.model.refdata;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Data
@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class BuildingLocation {
    String buildingLocationId;
    String buildingLocationName;
    String epimsId;
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
