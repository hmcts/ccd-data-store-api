package uk.gov.hmcts.ccd.datastore.befta.customvalue.handlers;

import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueHandler;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueKey;

import java.util.UUID;

import static uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueKey.GENERATE_UUID;

public class GenerateUuidHandler implements CustomValueHandler {

    @Override
    public Boolean matches(CustomValueKey key) {
        return GENERATE_UUID.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        return UUID.randomUUID();
    }

}

