package uk.gov.hmcts.ccd.domain.types;

import uk.gov.hmcts.ccd.ApplicationParams;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

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
