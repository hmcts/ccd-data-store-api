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
public class TextCaseReferenceCaseLinkValidator implements PredefinedTypeBaseTypeValidator {
    public static final String TYPE_ID = "CaseLink";
    public String predefinedFieldId = "TextCaseReference";

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
        return isAnExistingCase(dataValue, dataFieldId);
    }

    private List<ValidationResult> isAnExistingCase(final JsonNode dataValue, final String dataFieldId) {
        try {
            final String value = dataValue.textValue();
            this.caseService.getCaseDetailsByCaseReference(value);
            return Collections.emptyList();
        } catch (ResourceNotFoundException resourceNotFoundException) {
            return Collections.singletonList(
                new ValidationResult(
                    dataValue + " does not correspond to an existing CCD case. Please update before proceeding",
                    dataFieldId)
            );
        } catch (Exception exception){
            return Collections.emptyList();
        }
    }

    @Override
    public String getPredefinedFieldId() {
        return this.predefinedFieldId;
    }
}
