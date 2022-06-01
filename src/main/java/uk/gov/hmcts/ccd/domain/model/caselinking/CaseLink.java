package uk.gov.hmcts.ccd.domain.model.caselinking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
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

    private Boolean standardLink;
}
