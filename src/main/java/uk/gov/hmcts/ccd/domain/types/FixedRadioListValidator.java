package uk.gov.hmcts.ccd.domain.types;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class FixedRadioListValidator extends FixedListValidator {
    static final String TYPE_ID = "FixedRadioList";

    @Override
    public BaseType getType() {
        return BaseType.get(TYPE_ID);
    }

}
