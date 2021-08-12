package uk.gov.hmcts.ccd.domain.model.refdata;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Service implements Serializable {
    long serviceId;
    String orgUnit;
    String businessArea;
    String subBusinessArea;
    String jurisdiction;
    String serviceDescription;
    LocalDateTime lastUpdate;
    String serviceCode;
    String serviceShortDescription;
    String ccdServiceName;
    List<String> ccdCaseTypes;
}
