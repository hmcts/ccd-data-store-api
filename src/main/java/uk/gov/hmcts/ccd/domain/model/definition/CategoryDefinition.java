package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Data
public class CategoryDefinition implements Serializable {

    private String categoryId;
    private String categoryLabel;
    private String parentCategoryId;
    private LocalDate liveFrom;
    private LocalDate liveTo;
    private Integer displayOrder;
    private String caseTypeId;
}
