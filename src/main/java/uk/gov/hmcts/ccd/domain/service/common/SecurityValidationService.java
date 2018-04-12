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

    public void validateCallbackClassification(CallbackResponse callbackResponse, CaseDetails caseDetails) {
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
                return new ValidationException(VALIDATION_ERR_MSG);
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
                validateCallbackClassification(callbackClassificationValue.get(CLASSIFICATION), defaultClassificationItem.get(CLASSIFICATION));
                if (callbackClassificationValue.has(VALUE)) {
                    JsonNode defaultClassificationValue = defaultClassificationItem.get(VALUE);
                    JsonNode callbackClassificationItem = callbackClassificationValue.get(VALUE);
                    if (callbackClassificationItem.isObject()) {
                        validateObject(callbackClassificationItem, defaultClassificationValue);
                    } else {
                        validateCollection(callbackClassificationItem, defaultClassificationValue);
                    }
                } else {
                    LOG.warn("callbackClassification={} is complex object with classification but no value", defaultDataClassification, callbackDataClassification);
                    throw new ValidationException(VALIDATION_ERR_MSG);
                }
            } else if (callbackClassificationValue.has(VALUE)) {
                LOG.warn("callbackClassification={} is complex object with value but no classification", defaultDataClassification, callbackDataClassification);
                throw new ValidationException(VALIDATION_ERR_MSG);
            } else {
                validateCallbackClassification(callbackClassificationValue, defaultClassificationItem);
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
                LOG.warn("No defaultClassificationItem for callbackItem={} is null", callbackItem);
                throw new ValidationException(VALIDATION_ERR_MSG);
            }
            JsonNode callbackItemValue = callbackItem.get(VALUE);
            JsonNode defaultItemValue = defaultItem.get(VALUE);
            validateObject(callbackItemValue, defaultItemValue);
        }
    }

    private void validateCallbackClassification(JsonNode callbackClassificationValue, JsonNode defaultClassificationValue) {
        Optional<SecurityClassification> callbackSecurityClassification = getSecurityClassification(callbackClassificationValue);
        Optional<SecurityClassification> defaultSecurityClassification = getSecurityClassification(defaultClassificationValue);
        if (!defaultSecurityClassification.isPresent() || !callbackSecurityClassification.isPresent()
            || !callbackSecurityClassification.get().higherOrEqualTo(defaultSecurityClassification.get())) {
            LOG.warn("defaultClassificationValue={} has higher classification than callbackClassificationValue={} is null",
                     defaultClassificationValue,
                     callbackClassificationValue);
            throw new ValidationException(VALIDATION_ERR_MSG);
        }
    }

}
