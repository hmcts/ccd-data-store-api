package uk.gov.hmcts.ccd.datastore.befta.customvalue.handlers;

import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.ReflectionUtils;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueHandler;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueKey;

import static uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueKey.CASE_ID_AS_INTEGER;

public class CaseIdAsIntegerHandler implements CustomValueHandler {

    @Override
    public Boolean matches(CustomValueKey key) {
        return CASE_ID_AS_INTEGER.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        String childContext = key.toString().replace("caseIdAsIntegerFrom_","");
        try {
            //noinspection RedundantCast
            return (long) ReflectionUtils.deepGetFieldInObject(scenarioContext,
                "childContexts." + childContext + ".testData.actualResponse.body.id");
        } catch (Exception e) {
            throw new FunctionalTestException("Problem getting case id as long", e);
        }
    }

}