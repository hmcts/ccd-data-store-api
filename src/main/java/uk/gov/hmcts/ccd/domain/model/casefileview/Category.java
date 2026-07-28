package uk.gov.hmcts.ccd.domain.model.casefileview;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import lombok.Value;

import java.util.List;

@Value
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Category {
    String categoryId;
    String categoryName;
    Integer categoryOrder;
    List<Document> documents;
    List<Category> subCategories;
}
