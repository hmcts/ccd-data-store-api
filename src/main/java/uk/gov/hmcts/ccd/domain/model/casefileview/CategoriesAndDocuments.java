package uk.gov.hmcts.ccd.domain.model.casefileview;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import lombok.Value;

import java.util.List;

@Value
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CategoriesAndDocuments {
    Integer caseVersion;
    List<Category> categories;
    List<Document> uncategorisedDocuments;
}
