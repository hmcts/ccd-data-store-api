package uk.gov.hmcts.ccd.datastore.befta.customvalue.handlers;

import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.ccd.datastore.befta.DataStoreTestAutomationAdapter;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueHandler;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueKey;

import static uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueKey.UNIQUE_STRING;

public class UniqueStringHandler implements CustomValueHandler {

    private final DataStoreTestAutomationAdapter adapter;

    public UniqueStringHandler(DataStoreTestAutomationAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public Boolean matches(CustomValueKey key) {
        return UNIQUE_STRING.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        String scenarioTag;
        try {
            scenarioTag = scenarioContext.getParentContext().getCurrentScenarioTag();
        } catch (NullPointerException e) {
            scenarioTag = scenarioContext.getCurrentScenarioTag();
        }
        return this.adapter.getUniqueStringsPerScenario(scenarioTag);
    }

}

