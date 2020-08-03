package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;

@Named("CaseLinkValidator")
@Singleton
public class TextCaseReferenceCaseLinkValidator implements PredefinedTypeFieldValidator {

    public String predefinedFieldId = "TextCaseReference";
    private CaseService caseService;
    private TextValidator textValidator;

    @Inject
    public TextCaseReferenceCaseLinkValidator(TextValidator textValidator, CaseService caseService) {
        this.caseService = caseService;
    }

    @Override
    public List<ValidationResult> validate(final String dataFieldId,
                                           final JsonNode dataValue,
                                           final CaseFieldDefinition caseFieldDefinition) {

        List<ValidationResult> validationResults = textValidator.validate(dataFieldId, dataValue, caseFieldDefinition);
        if (validationResults.isEmpty()) {
            final String value = dataValue.textValue();
            return isAnExistingCase(value, dataFieldId);
        }
        return validationResults;
    }

    private List<ValidationResult> isAnExistingCase(final String value, final String dataFieldId) {
        try {
            this.caseService.getCaseDetailsByCaseReference(formatCaseReference(value));
            return Collections.emptyList();
        } catch (ResourceNotFoundException resourceNotFoundException) {
            return Collections.singletonList(
                new ValidationResult(
                    value + " does not correspond to an existing CCD case. Please update before proceeding",
                    dataFieldId)
            );
        } catch (Exception exception) {
            return Collections.emptyList();
        }
    }

    private String formatCaseReference(String caseReference) {
        if (caseReference.contains("-")) {
            return String.join("", caseReference.split("-"));
        }
        return caseReference;
    }

    @Override
    public String getPredefinedFieldId() {
        return this.predefinedFieldId;
    }
}
