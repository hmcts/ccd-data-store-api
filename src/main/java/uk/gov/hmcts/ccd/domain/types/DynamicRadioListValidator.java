package uk.gov.hmcts.ccd.domain.types;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Named
@Singleton
public class DynamicRadioListValidator extends DynamicListValidator {
    protected static final String TYPE_ID = "DynamicRadioList";

    @Override
    public BaseType getType() {
        return BaseType.get(TYPE_ID);
    }

}
