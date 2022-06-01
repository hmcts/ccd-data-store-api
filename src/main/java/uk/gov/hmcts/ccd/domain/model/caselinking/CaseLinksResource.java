package uk.gov.hmcts.ccd.domain.model.caselinking;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CaseLinksResource {
    private List<CaseLink> caseLinks;
}
