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
import static uk.gov.hmcts.ccd.domain.types.TextValidator.checkRegex;

@Named("CaseLinkValidator")
@Singleton
public class TextCaseReferenceCaseLinkValidator implements PredefinedTypeBaseTypeValidator {
    public static final String TYPE_ID = "CaseLink";
    public String predefinedFieldId = "TextCaseReference";
    private static final String REGULAR_EXPRESSION = "(?:^[0-9]{16}$|^\\d{4}-\\d{4}-\\d{4}-\\d{4}$)";
    private CaseService caseService;

    @Inject
    public TextCaseReferenceCaseLinkValidator(CaseService caseService) {
        this.caseService = caseService;
    }

    @Override
    public BaseType getType() {
        return BaseType.get(TYPE_ID);
    }

    @Override
    public List<ValidationResult> validate(final String dataFieldId,
                                           final JsonNode dataValue,
                                           final CaseFieldDefinition caseFieldDefinition) {

        if (isNullOrEmpty(dataValue)) {
            return Collections.emptyList();
        }

        final String value = dataValue.textValue();
        if (!checkRegex(REGULAR_EXPRESSION, value)) {
            return Collections.singletonList(new ValidationResult(REGEX_GUIDANCE, dataFieldId));
        }
        return isAnExistingCase(value, dataFieldId);
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
