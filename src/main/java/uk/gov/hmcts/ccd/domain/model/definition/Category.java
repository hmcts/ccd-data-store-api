package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Data
public class Category implements Serializable {

    private String categoryId;
    private String categoryLabel;
    private String parentCategoryId;
    private Date liveFrom;
    private Date liveTo;
    private String displayOrder;
    private String caseTypeId;
}
