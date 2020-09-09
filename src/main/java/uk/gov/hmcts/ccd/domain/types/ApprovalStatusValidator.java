package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import javax.inject.Named;
import javax.inject.Singleton;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

@Named
@Singleton
public class ApprovalStatusValidator implements PredefinedTypeFieldValidator {
    private static final String APPROVAL_STATUS = "ApprovalStatus";
    private NumberValidator numberValidator;

    @Autowired
    ApprovalStatusValidator(NumberValidator numberValidator) {
        this.numberValidator = numberValidator;
    }

    @Override
    public List<ValidationResult> validate(final String dataFieldId,
                                           final JsonNode dataValue,
                                           final CaseFieldDefinition caseFieldDefinition) {
        if (!APPROVAL_STATUS.equalsIgnoreCase(caseFieldDefinition.getId())
            || this.numberValidator.isNullOrEmpty(dataValue)) {
            return Collections.emptyList();
        }

        List<ValidationResult> validationResults = this.numberValidator.validate(dataFieldId, dataValue, caseFieldDefinition);

        if (validationResults.isEmpty()) {
            final String value = dataValue.textValue();
            final int numberValue = new BigDecimal(value).intValue();
            if (numberValue < 0 || numberValue > 2) {
                return Collections.singletonList(
                    new ValidationResult("Invalid Approval Status Value, Valid values are 0,1 and 2. "
                        + "0 = ‘Not considered’,"
                        + " 1 = ‘Approved’,"
                        + " 2 = ‘Rejected’", dataFieldId)
                );
            }
        }
        return validationResults;
    }

    @Override
    public String getPredefinedFieldId() {
        return APPROVAL_STATUS;
    }
}
