package uk.gov.hmcts.ccd.domain.model.search.elasticsearch;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.processor.DisplayContextParameter;
import uk.gov.hmcts.ccd.domain.service.processor.DisplayContextParameterType;

import java.util.Optional;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class SearchResultViewColumn {

    @JsonProperty("case_field_id")
    private String caseFieldId;
    @JsonProperty("case_field_type")
    private FieldTypeDefinition caseFieldTypeDefinition;
    private String label;
    private Integer order;
    private boolean metadata;
    @JsonProperty("display_context_parameter")
    private String displayContextParameter;
}
