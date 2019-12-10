package uk.gov.hmcts.ccd.fta.steps;

import org.assertj.core.util.Arrays;

import java.util.List;
import java.util.Map;

import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.fta.data.HttpTestData;
import uk.gov.hmcts.ccd.fta.data.RequestData;
import uk.gov.hmcts.ccd.fta.data.UserData;
import uk.gov.hmcts.ccd.fta.exception.FunctionalTestException;
import uk.gov.hmcts.ccd.fta.util.ReflectionUtils;

public class DynamicValueInjector {

    private static final String DYNAMIC_CONTENT_PLACEHOLDER = "[[DYNAMIC]]";
    private final AATHelper aat;

    private BackEndFunctionalTestScenarioContext scenarioContext;
    private HttpTestData testData;

    public DynamicValueInjector(AATHelper aat, HttpTestData testData,
            BackEndFunctionalTestScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
        this.testData = testData;
        this.aat = aat;
    }

    public void injectDataFromContext() {
        injectRequestDetailsFromContext();
    }

    private void injectRequestDetailsFromContext() {
        RequestData requestData = testData.getRequest();
        Map<String, Object> requestHeaders = requestData.getHeaders();
        if (requestHeaders != null) {
            requestHeaders.forEach((header, value) -> requestHeaders.put(header,
                    getDynamicValueFor("request.headers", header, value)));
        }

        Map<String, Object> pathVariables = requestData.getPathVariables();
        if (pathVariables != null) {
            pathVariables.forEach(
                    (key, value) -> pathVariables.put(key, getDynamicValueFor("request.pathVariables", key, value)));
        }

        Map<String, Object> queryParams = requestData.getQueryParams();
        if (queryParams != null) {
            queryParams.forEach(
                    (key, value) -> queryParams.put(key, getDynamicValueFor("request.queryParams", key, value)));
        }
        injectDynamicValuesInto("request.body", requestData.getBody());
    }

    private Object getDynamicValueFor(String path, String key, Object value) {
        if (value == null || !(value instanceof String))
            return value;

        String valueString = (String) value;
        if (valueString.equalsIgnoreCase(DYNAMIC_CONTENT_PLACEHOLDER)) {
            UserData theInvokingUser = scenarioContext.getTheInvokingUser();
            String s2sToken = null;
            if (key.equalsIgnoreCase("Authorization")) {
                return "Bearer " + theInvokingUser.getToken();
            } else if (key.equalsIgnoreCase("ServiceAuthorization")) {
                if ((s2sToken = aat.getS2SHelper().getToken()) != null) {
                    return s2sToken;
                }
            } else if (key.equalsIgnoreCase("uid") && theInvokingUser.getUid() != null) {
                return theInvokingUser.getUid();
            } else if (key.equalsIgnoreCase("cid")) {
                return calculateFromContext(scenarioContext,
                        "${[scenarioContext][childContexts][Standard_Full_Case_Creation_Data][testData][actualResponse][body][caseReference]}");
            }
            throw new FunctionalTestException("Dynamic value for '" + path + "." + key + "' does not exist!");
        } else if (isFormula(valueString)) {
            return calculateFromContext(scenarioContext, valueString);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private void injectDynamicValuesInto(String path, Map<String, Object> map) {
        if (map == null) {
            return;
        }
        map.forEach((key, value) -> {
            if (value instanceof String) {
                if (isFormula((String) value)) {
                    map.put(key, calculateFromContext(scenarioContext, (String) value));
                }
            } else if (Arrays.isArray(value)) {
                injectDynamicValuesInto(path + "." + key, (Object[]) value);
            } else if (value instanceof Map<?, ?>) {
                injectDynamicValuesInto(path + "." + key, (Map<String, Object>) value);
            }
        });

    }

    @SuppressWarnings("unchecked")
    private void injectDynamicValuesInto(String path, Object[] objects) {
        if (objects == null) {
            return;
        }
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] instanceof String) {
                if (isFormula((String) objects[i])) {
                    objects[i] = calculateFromContext(scenarioContext, (String) objects[i]);
                }
            } else if (objects[i] instanceof Map<?, ?>) {
                injectDynamicValuesInto(path + "[" + i + "]", (Map<String, Object>) objects[i]);
            } else if (Arrays.isArray(objects[i])) {
                injectDynamicValuesInto(path + "[" + i + "]", (Object[]) objects[i]);
            }
        }
    }

    private boolean isFormula(String valueString) {
        return valueString != null && valueString.startsWith("${") && valueString.endsWith("}");
    }

    private Object calculateFromContext(Object container, String formula) {
        String[] fields = formula.substring(3).split("\\]\\[|\\]\\}");
        return calculateInContainer(container, fields, 1);
    }

    private Object calculateInContainer(Object container, String[] fields, int fieldIndex) {
        Object value = null;
        if (Arrays.isArray(container)) {
            value = ((Object[]) container)[Integer.parseInt(fields[fieldIndex])];
        } else if (container instanceof List<?>) {
            value = ((List<?>) container).get(Integer.parseInt(fields[fieldIndex]));
        } else if (container instanceof Map<?, ?>) {
            value = ((Map<?, ?>) container).get(fields[fieldIndex]);
        } else {
            try {
                value = ReflectionUtils.retrieveFieldInObject(container, fields[fieldIndex]);
            } catch (Exception e) {
                throw new FunctionalTestException(e.getMessage());
            }
        }
        if (fieldIndex == fields.length - 1) {
            return value;
        } else {
            return calculateInContainer(value, fields, fieldIndex + 1);
        }

    }

}
