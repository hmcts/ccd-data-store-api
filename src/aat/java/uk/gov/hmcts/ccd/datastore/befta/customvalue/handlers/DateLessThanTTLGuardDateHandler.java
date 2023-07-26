package uk.gov.hmcts.ccd.datastore.befta.customvalue.handlers;

import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueHandler;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueKey;

import java.time.LocalDate;

import static uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueKey.DATE_LESS_THAN_TTL_GUARD_DATE;

public class DateLessThanTTLGuardDateHandler implements CustomValueHandler {

    @Override
    public Boolean matches(CustomValueKey key) {
        return DATE_LESS_THAN_TTL_GUARD_DATE.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        return LocalDate.now().plusDays(10).toString();
    }

}
