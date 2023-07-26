package uk.gov.hmcts.ccd.datastore.befta.customvalue.handlers;

import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.ReflectionUtils;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueHandler;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueKey;

import static uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueKey.CONTAINS;

public class ContainsHandler implements CustomValueHandler {

    @Override
    public Boolean matches(CustomValueKey key) {
        return CONTAINS.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        try {
            String actualValueStr = (String) ReflectionUtils.deepGetFieldInObject(scenarioContext,
                "testData.actualResponse.body.__plainTextValue__");
            String expectedValueStr = key.toString().replace("contains ", "");

            if (actualValueStr.contains(expectedValueStr)) {
                return actualValueStr;
            }
            return "expectedValueStr " + expectedValueStr + " not present in response ";
        } catch (Exception e) {
            throw new FunctionalTestException("Problem checking acceptable response payload: ", e);
        }
    }

}
