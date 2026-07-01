package uk.gov.hmcts.ccd.domain.types;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Named
@Singleton
public class TextAreaValidator extends TextValidator {
    static final String TYPE_ID = "TextArea";

    @Override
    public BaseType getType() {
        return BaseType.get(TYPE_ID);
    }
}
