package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.io.Serializable;
import java.time.LocalDate;

@Value
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PACKAGE)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CategoryDefinition implements Serializable {

    String categoryId;
    String categoryLabel;
    String parentCategoryId;
    LocalDate liveFrom;
    LocalDate liveTo;
    Integer displayOrder;
    String caseTypeId;

}
