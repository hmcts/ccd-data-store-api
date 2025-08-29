package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CategoryDefinition implements Serializable, Copyable<CategoryDefinition> {

    private String categoryId;
    private String categoryLabel;
    private String parentCategoryId;
    private LocalDate liveFrom;
    private LocalDate liveTo;
    private Integer displayOrder;
    private String caseTypeId;

    @JsonIgnore
    @Override
    public CategoryDefinition createCopy() {
        return new CategoryDefinition(
            this.categoryId,
            this.categoryLabel,
            this.parentCategoryId,
            this.liveFrom != null ? LocalDate.from(this.liveFrom) : null,
            this.liveTo != null ? LocalDate.from(this.liveTo) : null,
            this.displayOrder,
            this.caseTypeId
        );
    }
}
