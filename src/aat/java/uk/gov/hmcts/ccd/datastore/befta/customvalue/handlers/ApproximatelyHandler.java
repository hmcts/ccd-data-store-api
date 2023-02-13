package uk.gov.hmcts.ccd.datastore.befta.customvalue.handlers;

import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.ReflectionUtils;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueHandler;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueKey;

import static uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueKey.APPROXIMATELY;

public class ApproximatelyHandler implements CustomValueHandler {

    @Override
    public Boolean matches(CustomValueKey key) {
        return APPROXIMATELY.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        try {
            String actualSizeFromHeaderStr = (String) ReflectionUtils.deepGetFieldInObject(scenarioContext,
                "testData.actualResponse.headers.Content-Length");
            String expectedSizeStr = key.toString().replace("approximately ", "");

            int actualSize =  Integer.parseInt(actualSizeFromHeaderStr);
            int expectedSize = Integer.parseInt(expectedSizeStr);

            if (Math.abs(actualSize - expectedSize) < (actualSize * 10 / 100)) {
                return actualSizeFromHeaderStr;
            }
            return expectedSize;
        } catch (Exception e) {
            throw new FunctionalTestException("Problem checking acceptable response payload: ", e);
        }
    }

}
