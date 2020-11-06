package uk.gov.hmcts.ccd.domain.types;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

public interface FieldValidator {
    List<ValidationResult> validate(ValidationContext validationContext);
}
