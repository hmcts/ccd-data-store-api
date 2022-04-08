package uk.gov.hmcts.ccd.domain.model.casedeletion;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonNaming(PropertyNamingStrategy.UpperCamelCaseStrategy.class)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CaseLink {

    private Long caseReference;
    private Long linkedCaseReference;

    @JsonIgnore
    private Long caseId;
    @JsonIgnore
    private Long linkedCaseId;
    private String caseTypeId;
}
