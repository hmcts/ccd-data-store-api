package uk.gov.hmcts.ccd.domain.types;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Named
@Singleton
public class FixedRadioListValidator extends FixedListValidator {
    static final String TYPE_ID = "FixedRadioList";

    @Override
    public BaseType getType() {
        return BaseType.get(TYPE_ID);
    }

}
