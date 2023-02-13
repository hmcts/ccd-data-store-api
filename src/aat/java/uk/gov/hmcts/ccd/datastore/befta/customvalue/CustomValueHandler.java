package uk.gov.hmcts.ccd.datastore.befta.customvalue;

import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;

public interface CustomValueHandler {
    Boolean matches(CustomValueKey key);

    Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key);
}
