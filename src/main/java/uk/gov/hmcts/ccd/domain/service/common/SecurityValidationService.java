package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.getcase.DefaultGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.ClassifiedGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.AuthorisedGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.RestrictedGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils.caseHasClassificationEqualOrLowerThan;
import static uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils.getDataClassificationForData;
import static uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils.getSecurityClassification;
import static uk.gov.hmcts.ccd.endpoint.std.TestController.jcLog;

@Service
public class SecurityValidationService {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityValidationService.class);
    private static final String VALUE = "value";
    private static final String CLASSIFICATION = "classification";
    private static final String VALIDATION_ERR_MSG = "The event cannot be complete due to a callback returned data "
        + "validation error (c)";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DefaultGetCaseOperation defaultGetCaseOperation;

    @Autowired
    private ClassifiedGetCaseOperation classifiedGetCaseOperation;

    @Autowired
    private AuthorisedGetCaseOperation authorisedGetCaseOperation;

    @Autowired
    private RestrictedGetCaseOperation restrictedGetCaseOperation;

    @Autowired
    private CreatorGetCaseOperation creatorGetCaseOperation;

    private void jcLogJsonNodeValue(final String message, final JsonNode value) {
        try {
            jcLog(message + " " + value.size() + " " + value.hashCode() + " "
                + objectMapper.writeValueAsString(value).hashCode());
        } catch (Exception e) {
            jcLog(message + " EXCEPTION: " + e.getMessage());
        }
    }

    private void jcTestHarness(final JsonNode callbackDataClassificationValue, final String caseReference) {
        CaseDetails defaultCaseDetails = defaultGetCaseOperation.execute(caseReference).get();
        CaseDetails classifiedCaseDetails = classifiedGetCaseOperation.execute(caseReference).get();
        CaseDetails authorisedCaseDetails = authorisedGetCaseOperation.execute(caseReference).get();
        CaseDetails restrictedCaseDetails = restrictedGetCaseOperation.execute(caseReference).get();
        CaseDetails creatorCaseDetails = creatorGetCaseOperation.execute(caseReference).get();
        final JsonNode defaultDataClassification_Value =
            JacksonUtils.convertValueJsonNode(defaultCaseDetails.getDataClassification());
        final JsonNode classifiedDataClassification_Value =
            JacksonUtils.convertValueJsonNode(classifiedCaseDetails.getDataClassification());
        final JsonNode authorisedDataClassification_Value =
            JacksonUtils.convertValueJsonNode(authorisedCaseDetails.getDataClassification());
        final JsonNode restrictedDataClassification_Value =
            JacksonUtils.convertValueJsonNode(restrictedCaseDetails.getDataClassification());
        final JsonNode creatorDataClassification_Value =
            JacksonUtils.convertValueJsonNode(creatorCaseDetails.getDataClassification());

        try {
            validateObject(callbackDataClassificationValue, defaultDataClassification_Value);
            jcLog("JCDEBUG2: SecurityValidationService.jcTestHarness(): OK");
        } catch (Exception e) {
            jcLog("JCDEBUG2: SecurityValidationService.jcTestHarness(): ERROR: " + e.getMessage());
        }

        try {
            validateObject(callbackDataClassificationValue, classifiedDataClassification_Value);
            jcLog("JCDEBUG2: SecurityValidationService.jcTestHarness(): OK");
        } catch (Exception e) {
            jcLog("JCDEBUG2: SecurityValidationService.jcTestHarness(): ERROR: " + e.getMessage());
        }

        try {
            validateObject(callbackDataClassificationValue, authorisedDataClassification_Value);
            jcLog("JCDEBUG2: SecurityValidationService.jcTestHarness(): OK");
        } catch (Exception e) {
            jcLog("JCDEBUG2: SecurityValidationService.jcTestHarness(): ERROR: " + e.getMessage());
        }

        try {
            validateObject(callbackDataClassificationValue, restrictedDataClassification_Value);
            jcLog("JCDEBUG2: SecurityValidationService.jcTestHarness(): OK");
        } catch (Exception e) {
            jcLog("JCDEBUG2: SecurityValidationService.jcTestHarness(): ERROR: " + e.getMessage());
        }

        try {
            validateObject(callbackDataClassificationValue, creatorDataClassification_Value);
            jcLog("JCDEBUG2: SecurityValidationService.jcTestHarness(): OK");
        } catch (Exception e) {
            jcLog("JCDEBUG2: SecurityValidationService.jcTestHarness(): ERROR: " + e.getMessage());
        }
    }

    /*
     * 1. CallbackInvoker.validateAndSetFromAboutToSubmitCallback
     * 2. SecurityValidationService.setClassificationFromCallbackIfValid
     * 3. SecurityValidationService.validateObject
     *
     * Debugging :-
     * "JCDEBUG2: SecurityValidationService.setClassificationFromCallbackIfValid()"
     * "JCDEBUG2: DefaultGetCaseOperation.execute()"
     * "JCDEBUG2: SecurityValidationService.jcTestHarness()"
     */
    public void setClassificationFromCallbackIfValid(final CallbackResponse callbackResponse,
                                                     final CaseDetails caseDetails,
                                                     final Map<String, JsonNode> defaultDataClassification) {

        if (caseHasClassificationEqualOrLowerThan(callbackResponse.getSecurityClassification()).test(caseDetails)) {
            caseDetails.setSecurityClassification(callbackResponse.getSecurityClassification());

            // BELOW: JC debugging
            final JsonNode callbackDataClassification_Value =
                JacksonUtils.convertValueJsonNode(callbackResponse.getDataClassification());
            final JsonNode defaultDataClassification_Value =
                JacksonUtils.convertValueJsonNode(defaultDataClassification);
            jcLogJsonNodeValue("JCDEBUG2: SecurityValidationService.setClassificationFromCallbackIfValid(): "
                + "callbackDataClassification_Value", callbackDataClassification_Value);
            jcLogJsonNodeValue("JCDEBUG2: SecurityValidationService.setClassificationFromCallbackIfValid(): "
                + "defaultDataClassification_Value", defaultDataClassification_Value);
            try {
                jcTestHarness(callbackDataClassification_Value, caseDetails.getReferenceAsString());
            } catch (Exception e) {
                jcLog("JCDEBUG2: SecurityValidationService.jcTestHarness(): *FAIL*" + e.getMessage());
            }
            // ABOVE: JC debugging

            jcLog("JCDEBUG2: SecurityValidationService.setClassificationFromCallbackIfValid(): BEFORE VALIDATE OBJECT");
            try {
                validateObject(callbackDataClassification_Value, defaultDataClassification_Value);
            } catch (Exception e) {
                final JsonNode defaultDataClassification_Value2;
                try {
                    CaseDetails defaultCaseDetails =
                        defaultGetCaseOperation.execute(caseDetails.getReferenceAsString()).get();
                    defaultDataClassification_Value2 =
                        JacksonUtils.convertValueJsonNode(defaultCaseDetails.getDataClassification());
                } catch (Exception e2) {
                    throw new ValidationException(VALIDATION_ERR_MSG);
                }
                validateObject(callbackDataClassification_Value, defaultDataClassification_Value2);
            }
            jcLog("JCDEBUG2: SecurityValidationService.setClassificationFromCallbackIfValid(): AFTER VALIDATE OBJECT");

            caseDetails.setDataClassification(JacksonUtils.convertValue(callbackResponse.getDataClassification()));

            jcLog("JCDEBUG2: SecurityValidationService.setClassificationFromCallbackIfValid(): AFTER SET DATA CLASS.");
        } else {
            LOG.warn("CallbackCaseClassification={} has lower classification than caseClassification={} for "
                    + "caseReference={}, jurisdiction={} and caseType={}",
                callbackResponse.getSecurityClassification(),
                caseDetails.getSecurityClassification(),
                caseDetails.getReference(),
                caseDetails.getJurisdiction(),
                caseDetails.getCaseTypeId());
            throw new ValidationException(VALIDATION_ERR_MSG);
        }
    }

    private void validateObject(final JsonNode callbackDataClassification, final JsonNode defaultDataClassification) {

        if (!isNotNullAndSizeEqual(callbackDataClassification, defaultDataClassification)) {
            LOG.warn("callbackClassification={} and defaultClassification={} sizes differ", callbackDataClassification,
                defaultDataClassification);
            throw new ValidationException(VALIDATION_ERR_MSG);
        }

        Iterator<Map.Entry<String, JsonNode>> callbackDataClassificationIterator = callbackDataClassification.fields();
        while (callbackDataClassificationIterator.hasNext()) {
            Map.Entry<String, JsonNode> callbackClassificationMap = callbackDataClassificationIterator.next();
            String callbackClassificationKey = callbackClassificationMap.getKey();
            JsonNode callbackClassificationValue = callbackClassificationMap.getValue();
            JsonNode defaultClassificationItem = defaultDataClassification.get(callbackClassificationKey);
            if (callbackClassificationValue.has(CLASSIFICATION)) {
                if (!isValidClassification(callbackClassificationValue.get(CLASSIFICATION),
                    defaultClassificationItem.get(CLASSIFICATION))) {
                    LOG.warn("callbackClassificationItem={} has lower classification than defaultClassificationItem={}",
                        callbackClassificationValue,
                        defaultClassificationItem);
                    throw new ValidationException(VALIDATION_ERR_MSG);
                }
                if (callbackClassificationValue.has(VALUE)) {
                    JsonNode defaultClassificationValue = defaultClassificationItem.get(VALUE);
                    JsonNode callbackClassificationItem = callbackClassificationValue.get(VALUE);
                    if (callbackClassificationItem.isObject()) {
                        validateObject(callbackClassificationItem, defaultClassificationValue);
                    } else {
                        validateCollection(callbackClassificationItem, defaultClassificationValue);
                    }
                } else {
                    LOG.warn("callbackClassification={} is complex object with classification but no value",
                        callbackDataClassification);
                    throw new ValidationException(VALIDATION_ERR_MSG);
                }
            } else if (callbackClassificationValue.has(VALUE)) {
                LOG.warn("callbackClassification={} is complex object with value but no classification",
                    callbackDataClassification);
                throw new ValidationException(VALIDATION_ERR_MSG);
            } else {
                if (!isValidClassification(callbackClassificationValue, defaultClassificationItem)) {
                    LOG.warn("callbackClassificationItem={} has lower classification than defaultClassificationItem={}",
                        JacksonUtils.convertValueJsonNode(callbackClassificationMap),
                        defaultDataClassification);
                    throw new ValidationException(VALIDATION_ERR_MSG);
                }
            }
        }
    }

    private boolean isNotNullAndSizeEqual(JsonNode callbackDataClassification, JsonNode defaultDataClassification) {
        return defaultDataClassification != null && callbackDataClassification != null
            && defaultDataClassification.size() == callbackDataClassification.size();
    }


    private void validateCollection(JsonNode callbackClassificationItem, JsonNode defaultClassificationItem) {
        for (JsonNode callbackItem : callbackClassificationItem) {
            JsonNode defaultItem = getDataClassificationForData(callbackItem, defaultClassificationItem.iterator());
            if (defaultItem.isNull()) {
                LOG.warn("No defaultClassificationItem for callbackItem={}", callbackItem);
                throw new ValidationException(VALIDATION_ERR_MSG);
            }
            JsonNode callbackItemValue = callbackItem.get(VALUE);
            JsonNode defaultItemValue = defaultItem.get(VALUE);
            validateObject(callbackItemValue, defaultItemValue);
        }
    }

    private boolean isValidClassification(JsonNode callbackClassificationValue, JsonNode defaultClassificationValue) {
        Optional<SecurityClassification> callbackSecurityClassification =
            getSecurityClassification(callbackClassificationValue);
        Optional<SecurityClassification> defaultSecurityClassification =
            getSecurityClassification(defaultClassificationValue);
        if (!defaultSecurityClassification.isPresent()) {
            LOG.warn("defaultSecurityClassificationValue={} cannot be parsed", defaultClassificationValue);
            throw new ValidationException(VALIDATION_ERR_MSG);
        }
        if (!callbackSecurityClassification.isPresent()) {
            LOG.warn("callbackSecurityClassificationValue={} cannot be parsed", callbackClassificationValue);
            throw new ValidationException(VALIDATION_ERR_MSG);
        }
        return callbackSecurityClassification.get().higherOrEqualTo(defaultSecurityClassification.get());
    }

}
