package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils.caseHasClassificationEqualOrLowerThan;
import static uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils.getDataClassificationForData;
import static uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils.getSecurityClassification;

@Service
public class SecurityValidationService {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityValidationService.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference STRING_JSON_MAP = new TypeReference<HashMap<String, JsonNode>>() {
    };
    private static final String VALUE = "value";
    private static final String CLASSIFICATION = "classification";
    private static final String VALIDATION_ERR_MSG = "The event cannot be completed as something went wrong while updating the security level of the case or some of the case fields";
    private static final String CASE_SECURITY_LEVEL_TOO_LOW_MSG = "The security level of the case with reference=%s cannot be loosened";
    private static final String CASE_DATA_SECURITY_LEVEL_TOO_LOW_MSG = "The security level of the caseData=%s cannot be loosened";

    public void setClassificationFromCallbackIfValid(CallbackResponse callbackResponse, CaseDetails caseDetails) {
        Optional<CaseDetails> result = Optional.of(caseDetails);

        result.filter(caseHasClassificationEqualOrLowerThan(callbackResponse.getSecurityClassification()))
            .map(cd -> {
                cd.setSecurityClassification(callbackResponse.getSecurityClassification());

                validateObject(MAPPER.convertValue(callbackResponse.getDataClassification(), JsonNode.class),
                               MAPPER.convertValue(caseDetails.getDataClassification(), JsonNode.class));

                caseDetails.setDataClassification(MAPPER.convertValue(callbackResponse.getDataClassification(), STRING_JSON_MAP));
                return cd;
            })
            .orElseThrow(() -> {
                LOG.warn("Case={} classification is higher than callbackResponse={} case classification", caseDetails, callbackResponse);
                return new ValidationException(String.format(CASE_SECURITY_LEVEL_TOO_LOW_MSG, caseDetails.getReference()));
            });
    }

    private void validateObject(JsonNode callbackDataClassification, JsonNode defaultDataClassification) {

        if (!isNotNullAndSizeEqual(callbackDataClassification, defaultDataClassification)) {
            LOG.warn("defaultClassification={} and callbackClassification={} sizes differ", defaultDataClassification, callbackDataClassification);
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
                    LOG.warn("defaultClassificationItem={} has higher classification than callbackClassificationItem={}",
                             defaultClassificationItem,
                             callbackClassificationValue);
                    throw new ValidationException(String.format(CASE_DATA_SECURITY_LEVEL_TOO_LOW_MSG, callbackClassificationKey));
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
                    LOG.warn("defaultClassificationItem={} has higher classification than callbackClassificationItem={}",
                             defaultDataClassification,
                             MAPPER.convertValue(callbackClassificationMap, JsonNode.class));
                    throw new ValidationException(String.format(CASE_DATA_SECURITY_LEVEL_TOO_LOW_MSG, callbackClassificationKey));
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
        if(!callbackSecurityClassification.isPresent()) {
            LOG.warn("callbackSecurityClassificationValue={} cannot be parsed", callbackClassificationValue);
            throw new ValidationException(VALIDATION_ERR_MSG);
        }
        if(!callbackSecurityClassification.get().higherOrEqualTo(defaultSecurityClassification.get())) {
            return false;
        }
        return true;
    }

}
