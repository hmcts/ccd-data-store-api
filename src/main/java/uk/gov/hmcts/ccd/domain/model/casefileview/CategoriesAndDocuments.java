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
    Document theDocument;

    public CategoriesAndDocuments(Integer caseVersion,
        List<Category> categories,
        List<uk.gov.hmcts.ccd.domain.model.casefileview.Document> uncategorisedDocuments) {
        this.caseVersion = caseVersion;
        this.categories = categories;
        this.uncategorisedDocuments = uncategorisedDocuments;
        this.theDocument = null;
    }

    @ApiModelProperty(dataType = "java.util.List<uk.gov.hmcts.ccd.domain.model.casefileview.Document>")
    public List<Document> getUncategorisedDocuments() {
        return this.uncategorisedDocuments;
    }

    @ApiModelProperty(dataType = "uk.gov.hmcts.ccd.domain.model.casefileview.Document")
    public Document getTheDocument() {
        return this.theDocument;
    }

}
