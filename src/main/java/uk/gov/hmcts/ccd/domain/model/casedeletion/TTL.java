package uk.gov.hmcts.ccd.domain.model.casedeletion;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@JsonNaming(PropertyNamingStrategy.UpperCamelCaseStrategy.class)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TTL {

    public static final String TTL_CASE_FIELD_ID = "TTL";

    private LocalDate systemTTL;

    private LocalDate overrideTTL;

    private String suspended;
}
