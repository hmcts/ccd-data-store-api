package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class CategoryDefinition implements Serializable {

    String categoryId;
    String categoryLabel;
    String parentCategoryId;
    Date liveFrom;
    Date liveTo;
    String displayOrder;
    String caseTypeId;

}
