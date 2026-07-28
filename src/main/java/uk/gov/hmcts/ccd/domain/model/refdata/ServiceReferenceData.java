package uk.gov.hmcts.ccd.domain.model.refdata;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ServiceReferenceData implements Serializable {
    long serviceId;
    String orgUnit;
    String businessArea;
    String subBusinessArea;
    String jurisdiction;
    String serviceDescription;
    String serviceCode;
    String serviceShortDescription;
    String ccdServiceName;
    LocalDateTime lastUpdate;
    List<String> ccdCaseTypes;
}
