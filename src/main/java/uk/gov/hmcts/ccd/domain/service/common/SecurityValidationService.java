package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
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
    private DefaultGetCaseOperation defaultGetCaseOperation;

    @Autowired
    private ClassifiedGetCaseOperation classifiedGetCaseOperation;

    @Autowired
    private AuthorisedGetCaseOperation authorisedGetCaseOperation;

    @Autowired
    private RestrictedGetCaseOperation restrictedGetCaseOperation;

    @Autowired
    private CreatorGetCaseOperation creatorGetCaseOperation;

    private void jcTestHarness(final String caseReference) {
        try {
            CaseDetails defaultCaseDetails = defaultGetCaseOperation.execute(caseReference).get();
            CaseDetails classifiedCaseDetails = classifiedGetCaseOperation.execute(caseReference).get();
            CaseDetails authorisedCaseDetails = authorisedGetCaseOperation.execute(caseReference).get();
            CaseDetails restrictedCaseDetails = restrictedGetCaseOperation.execute(caseReference).get();
            CaseDetails creatorCaseDetails = creatorGetCaseOperation.execute(caseReference).get();
        } catch (Exception e) {
            // Empty
        }
    }

    public void setClassificationFromCallbackIfValid(final CallbackResponse callbackResponse,
                                                     final CaseDetails caseDetails,
                                                     final Map<String, JsonNode> defaultDataClassification) {

        if (caseHasClassificationEqualOrLowerThan(callbackResponse.getSecurityClassification()).test(caseDetails)) {
            caseDetails.setSecurityClassification(callbackResponse.getSecurityClassification());

            jcLog("setClassificationFromCallbackIfValid #1");

            // BELOW: JC debugging
            jcTestHarness(caseDetails.getReferenceAsString());
            // ABOVE: JC debugging

            jcLog("setClassificationFromCallbackIfValid #2");
            final JsonNode callbackDataClassification_Value =
                JacksonUtils.convertValueJsonNode(callbackResponse.getDataClassification());
            jcLog("setClassificationFromCallbackIfValid #3");
            final JsonNode defaultDataClassification_Value =
                JacksonUtils.convertValueJsonNode(defaultDataClassification);

            jcLog("setClassificationFromCallbackIfValid #4");
            try {
                jcLog("setClassificationFromCallbackIfValid #5");
                validateObject(callbackDataClassification_Value, defaultDataClassification_Value);
            } catch (Exception e) {
                jcLog("setClassificationFromCallbackIfValid #6");
                final JsonNode defaultDataClassification_Value2;
                try {
                    jcLog("setClassificationFromCallbackIfValid #7");
                    CaseDetails defaultCaseDetails =
                        defaultGetCaseOperation.execute(caseDetails.getReferenceAsString()).get();
                    jcLog("setClassificationFromCallbackIfValid #8");
                    defaultDataClassification_Value2 =
                        JacksonUtils.convertValueJsonNode(defaultCaseDetails.getDataClassification());
                } catch (Exception e2) {
                    jcLog("setClassificationFromCallbackIfValid #9");
                    throw new ValidationException(VALIDATION_ERR_MSG);
                }
                jcLog("setClassificationFromCallbackIfValid #10");
                validateObject(callbackDataClassification_Value, defaultDataClassification_Value2);
            }

            jcLog("setClassificationFromCallbackIfValid #11");
            caseDetails.setDataClassification(JacksonUtils.convertValue(callbackResponse.getDataClassification()));

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
