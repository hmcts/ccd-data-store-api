package uk.gov.hmcts.ccd.domain.model.casefileview;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Value;

import java.util.List;

@Value
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class CategoriesAndDocuments {
    Integer caseVersion;
    List<Category> categories;
    List<Document> uncategorisedDocuments;
}
