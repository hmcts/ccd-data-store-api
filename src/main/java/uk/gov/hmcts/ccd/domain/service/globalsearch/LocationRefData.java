package uk.gov.hmcts.ccd.domain.service.globalsearch;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LocationRefData {
    String locationId;
    String locationName;
    String regionId;
    String regionName;
}
