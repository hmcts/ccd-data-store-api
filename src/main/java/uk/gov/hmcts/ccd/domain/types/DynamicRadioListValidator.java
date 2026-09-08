package uk.gov.hmcts.ccd.domain.types;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import uk.gov.hmcts.ccd.ApplicationParams;

@Named
@Singleton
public class DynamicRadioListValidator extends DynamicListValidator {
    protected static final String TYPE_ID = "DynamicRadioList";

    @Inject
    public DynamicRadioListValidator(ApplicationParams applicationParams) {
        super(applicationParams);
    }

    @Override
    public BaseType getType() {
        return BaseType.get(TYPE_ID);
    }

}
