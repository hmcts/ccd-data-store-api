package uk.gov.hmcts.ccd.domain.types;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class TextAreaValidator extends TextValidator {
    static final String TYPE_ID = "TextArea";

    @Override
    public BaseType getType() {
        return BaseType.get(TYPE_ID);
    }
}
