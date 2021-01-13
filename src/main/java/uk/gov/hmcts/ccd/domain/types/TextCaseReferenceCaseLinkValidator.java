package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;

import static uk.gov.hmcts.ccd.domain.model.common.CaseReferenceUtils.formatCaseReference;

@Named
@Singleton
public class TextCaseReferenceCaseLinkValidator implements CustomTypeValidator {

    private static final Logger LOG = LoggerFactory.getLogger(TextCaseReferenceCaseLinkValidator.class);
    private CaseService caseService;
    private TextValidator textValidator;

    @Inject
    public TextCaseReferenceCaseLinkValidator(@Qualifier("TextValidator") TextValidator textValidator,
                                              CaseService caseService) {
        this.caseService = caseService;
        this.textValidator = textValidator;
    }

    @Override
    public List<ValidationResult> validate(ValidationContext validationContext) {

        final String dataFieldId = validationContext.getFieldId();
        final JsonNode dataValue = validationContext.getFieldValue();
        final CaseFieldDefinition caseFieldDefinition = validationContext.getFieldDefinition();
        final List<ValidationResult> validationResults =
            textValidator.validate(dataFieldId, dataValue, caseFieldDefinition);

        if (validationResults.isEmpty() && !textValidator.isNullOrEmpty(dataValue)) {
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
                    value + " does not correspond to an existing CCD case.",
                    dataFieldId)
            );
        } catch (Exception exception) {
            final String message = "Un expected error during case link validation.";
            LOG.error(message, exception);
            throw new ServiceException(message, exception);
        }
    }

    @Override
    public String getCustomTypeId() {
        return CustomTypes.CASE_LINK_TEXT_CASE_REFERENCE.getId();
    }
}
