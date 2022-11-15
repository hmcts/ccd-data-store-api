package uk.gov.hmcts.ccd.domain.model.casefileview;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.annotations.ApiModelProperty;
import lombok.Value;

import java.util.List;

@Value
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class CategoriesAndDocuments {
    Integer caseVersion;
    List<Category> categories;
    List<uk.gov.hmcts.ccd.domain.model.casefileview.Document> uncategorisedDocuments;

    @ApiModelProperty(dataType = "java.util.List<uk.gov.hmcts.ccd.domain.model.casefileview.Document>")
    public List<Document> getUncategorisedDocuments() {
        return this.uncategorisedDocuments;
    }
}
