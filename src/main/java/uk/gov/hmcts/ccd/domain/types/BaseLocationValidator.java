package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;

@Named("BaseLocationValidator")
@Singleton
public class BaseLocationValidator implements BaseTypeValidator {
    public static final String TYPE_ID = "BaseLocation";
    private TextValidator textValidator;

    @Inject
    public BaseLocationValidator(@Qualifier("TextValidator") TextValidator textValidator) {
        this.textValidator = textValidator;
    }

    @Override
    public BaseType getType() {
        return BaseType.get(TYPE_ID);
    }

    @Override
    public List<ValidationResult> validate(final String dataFieldId,
                                           final JsonNode dataValue,
                                           final CaseFieldDefinition caseFieldDefinition) {

        List<ValidationResult> validationResult = textValidator.validate(dataFieldId,dataValue,caseFieldDefinition);

        return validationResult;

    }

}
