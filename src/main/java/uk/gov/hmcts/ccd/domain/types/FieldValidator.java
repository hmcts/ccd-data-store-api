package uk.gov.hmcts.ccd.domain.types;

import java.util.List;

public interface FieldValidator {
    List<ValidationResult> validate(ValidationContext validationContext);
}
