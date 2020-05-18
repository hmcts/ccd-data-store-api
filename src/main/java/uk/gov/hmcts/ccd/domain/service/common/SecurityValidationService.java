package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils.caseHasClassificationEqualOrLowerThan;
import static uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils.getDataClassificationForData;
import static uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils.getSecurityClassification;

@Service
public class SecurityValidationService {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityValidationService.class);
    private static final String VALUE = "value";
    private static final String CLASSIFICATION = "classification";
    private static final String VALIDATION_ERR_MSG = "The event cannot be complete due to a callback returned data validation error (c)";

    public void setClassificationFromCallbackIfValid(CallbackResponse callbackResponse,
                                                     CaseDetails caseDetails,
                                                     Map<String, JsonNode> defaultDataClassification) {

        if (caseHasClassificationEqualOrLowerThan(callbackResponse.getSecurityClassification()).test(caseDetails)) {
            caseDetails.setSecurityClassification(callbackResponse.getSecurityClassification());

            validateObject(JacksonUtils.convertValueJsonNode(callbackResponse.getDataClassification()),
                JacksonUtils.convertValueJsonNode(defaultDataClassification));

            caseDetails.setDataClassification(JacksonUtils.convertValue(callbackResponse.getDataClassification()));
        } else {
            LOG.warn("CallbackCaseClassification={} has lower classification than caseClassification={} for caseReference={}, jurisdiction={} and caseType={}",
                callbackResponse.getSecurityClassification(),
                caseDetails.getSecurityClassification(),
                caseDetails.getReference(),
                caseDetails.getJurisdiction(),
                caseDetails.getCaseTypeId());
            throw new ValidationException(VALIDATION_ERR_MSG);
        }
    }

    private void validateObject(JsonNode callbackDataClassification, JsonNode defaultDataClassification) {

        if (!isNotNullAndSizeEqual(callbackDataClassification, defaultDataClassification)) {
            LOG.warn("callbackClassification={} and defaultClassification={} sizes differ", callbackDataClassification, defaultDataClassification);
            throw new ValidationException(VALIDATION_ERR_MSG);
        }

        Iterator<Map.Entry<String, JsonNode>> callbackDataClassificationIterator = callbackDataClassification.fields();
        while (callbackDataClassificationIterator.hasNext()) {
            Map.Entry<String, JsonNode> callbackClassificationMap = callbackDataClassificationIterator.next();
            String callbackClassificationKey = callbackClassificationMap.getKey();
            JsonNode callbackClassificationValue = callbackClassificationMap.getValue();
            JsonNode defaultClassificationItem = defaultDataClassification.get(callbackClassificationKey);
            if (callbackClassificationValue.has(CLASSIFICATION)) {
                if (!isValidClassification(callbackClassificationValue.get(CLASSIFICATION), defaultClassificationItem.get(CLASSIFICATION))) {
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
                    LOG.warn("callbackClassification={} is complex object with classification but no value", callbackDataClassification);
                    throw new ValidationException(VALIDATION_ERR_MSG);
                }
            } else if (callbackClassificationValue.has(VALUE)) {
                LOG.warn("callbackClassification={} is complex object with value but no classification", callbackDataClassification);
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
        return defaultDataClassification != null && callbackDataClassification != null &&
            defaultDataClassification.size() == callbackDataClassification.size();
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
        Optional<SecurityClassification> callbackSecurityClassification = getSecurityClassification(callbackClassificationValue);
        Optional<SecurityClassification> defaultSecurityClassification = getSecurityClassification(defaultClassificationValue);
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
