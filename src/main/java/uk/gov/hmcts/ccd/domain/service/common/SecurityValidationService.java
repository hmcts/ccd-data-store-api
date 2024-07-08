package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import uk.gov.hmcts.ccd.endpoint.std.TestController;

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

    private DefaultGetCaseOperation defaultGetCaseOperation;
    private ClassifiedGetCaseOperation classifiedGetCaseOperation;
    private AuthorisedGetCaseOperation authorisedGetCaseOperation;
    private RestrictedGetCaseOperation restrictedGetCaseOperation;
    private CreatorGetCaseOperation creatorGetCaseOperation;

    @Autowired
    public SecurityValidationService(@Qualifier("default") DefaultGetCaseOperation defaultGetCaseOperation,
                                     @Qualifier("classified") ClassifiedGetCaseOperation classifiedGetCaseOperation,
                                     @Qualifier("authorised") AuthorisedGetCaseOperation authorisedGetCaseOperation,
                                     @Qualifier("restricted") RestrictedGetCaseOperation restrictedGetCaseOperation,
                                     @Qualifier("creator") CreatorGetCaseOperation creatorGetCaseOperation) {
        this.defaultGetCaseOperation = defaultGetCaseOperation;
        this.classifiedGetCaseOperation = classifiedGetCaseOperation;
        this.authorisedGetCaseOperation = authorisedGetCaseOperation;
        this.restrictedGetCaseOperation = restrictedGetCaseOperation;
        this.creatorGetCaseOperation = creatorGetCaseOperation;
    }

    /*
     * ==== JC Test Harness. ====
     */
    private void jcTestHarness(final String caseReference) {
        try {
            int[] flags = TestController.getFlags();
            if (flags[0] == 1) {
                TestController.jcLog("jcTestHarness: defaultCaseDetails: YES");
                CaseDetails defaultCaseDetails = defaultGetCaseOperation.execute(caseReference).get();
            } else {
                TestController.jcLog("jcTestHarness: defaultCaseDetails: NO");
            }

            if (flags[1] == 1) {
                TestController.jcLog("jcTestHarness: classifiedCaseDetails: YES");
                CaseDetails classifiedCaseDetails = classifiedGetCaseOperation.execute(caseReference).get();
            } else {
                TestController.jcLog("jcTestHarness: classifiedCaseDetails: NO");
            }

            if (flags[2] == 1) {
                TestController.jcLog("jcTestHarness: authorisedCaseDetails: YES");
                CaseDetails authorisedCaseDetails = authorisedGetCaseOperation.execute(caseReference).get();
            } else {
                TestController.jcLog("jcTestHarness: authorisedCaseDetails: NO");
            }

            if (flags[3] == 1) {
                TestController.jcLog("jcTestHarness: restrictedCaseDetails: YES");
                CaseDetails restrictedCaseDetails = restrictedGetCaseOperation.execute(caseReference).get();
            } else {
                TestController.jcLog("jcTestHarness: restrictedCaseDetails: NO");
            }

            if (flags[4] == 1) {
                TestController.jcLog("jcTestHarness: creatorCaseDetails: YES");
                CaseDetails creatorCaseDetails = creatorGetCaseOperation.execute(caseReference).get();
            } else {
                TestController.jcLog("jcTestHarness: creatorCaseDetails: NO");
            }

        } catch (Exception e) {
            // Empty
        }
    }

    /*
     * ==== Copy of Validation From PR-2426. ====
     */
    private void copyOfValidationFromPr2426(final CallbackResponse callbackResponse,
                                            final CaseDetails caseDetails,
                                            final Map<String, JsonNode> deducedDataClassification) {
        // PR-2426  (13th June)
        try {
            TestController.jcLog("PR-2426: setClassificationFromCallbackIfValid #1");
            validateObject(JacksonUtils.convertValueJsonNode(callbackResponse.getDataClassification()),
                JacksonUtils.convertValueJsonNode(deducedDataClassification));
            TestController.jcLog("PR-2426: setClassificationFromCallbackIfValid #2  [deduced OK]");
        } catch (ValidationException deducedDataClassificationException) {
            TestController.jcLog("PR-2426: setClassificationFromCallbackIfValid #3  [deduced FAILED]");
            final Optional<CaseDetails> defaultCaseDetails;
            try {
                TestController.jcLog("PR-2426: setClassificationFromCallbackIfValid #4  [before get default]");
                defaultCaseDetails = classifiedGetCaseOperation.execute(caseDetails.getReferenceAsString());
                TestController.jcLog("PR-2426: setClassificationFromCallbackIfValid #5  [after get default]");
            } catch (Exception defaultDataClassificationException) {
                TestController.jcLog("PR-2426: setClassificationFromCallbackIfValid #6  [get default FAIL");
                throw new ValidationException(VALIDATION_ERR_MSG);
            }
            if (defaultCaseDetails.isEmpty()) {
                TestController.jcLog("PR-2426: setClassificationFromCallbackIfValid #7  [get default EMPTY]");
                throw new ValidationException(VALIDATION_ERR_MSG);
            } else {
                TestController.jcLog("PR-2426: setClassificationFromCallbackIfValid #8  [before validate default]");
                try {
                    validateObject(JacksonUtils.convertValueJsonNode(callbackResponse.getDataClassification()),
                        JacksonUtils.convertValueJsonNode(defaultCaseDetails.get().getDataClassification()));
                } catch (Exception e) {
                    TestController.jcLog("PR-2426: setClassificationFromCallbackIfValid #9  [EXCEPTION]");
                    TestController.jcLog("PR-2426: setClassificationFromCallbackIfValid #9  " + e.getMessage());
                    TestController.jcLog("PR-2426: setClassificationFromCallbackIfValid #9  "
                        + e.getStackTrace().toString());
                    throw e;
                }
                TestController.jcLog("PR-2426: setClassificationFromCallbackIfValid #10  [validate default OK]");
            }
        }
        // PR-2426  (13th June)
    }

    /*
     * ==== Set Classification From Callback If Valid ====
     */
    public void setClassificationFromCallbackIfValid(final CallbackResponse callbackResponse,
                                                     final CaseDetails caseDetails,
                                                     final Map<String, JsonNode> deducedDataClassification) {

        if (caseHasClassificationEqualOrLowerThan(callbackResponse.getSecurityClassification()).test(caseDetails)) {
            caseDetails.setSecurityClassification(callbackResponse.getSecurityClassification());

            jcLog("PR-2410: setClassificationFromCallbackIfValid #1  [DATE: 8th July]");

            // BELOW: JC debugging
            // jcTestHarness(caseDetails.getReferenceAsString());
            try {
                copyOfValidationFromPr2426(callbackResponse, caseDetails, deducedDataClassification);
            } catch (Exception e) {
                // Empty
            }
            // ABOVE: JC debugging

            jcLog("PR-2410: setClassificationFromCallbackIfValid #2");
            final JsonNode callbackDataClassification_Value =
                JacksonUtils.convertValueJsonNode(callbackResponse.getDataClassification());
            jcLog("PR-2410: setClassificationFromCallbackIfValid #3");
            final JsonNode deducedDataClassification_Value =
                JacksonUtils.convertValueJsonNode(deducedDataClassification);

            jcLog("PR-2410: setClassificationFromCallbackIfValid #4");
            try {
                jcLog("PR-2410: setClassificationFromCallbackIfValid #5");
                validateObject(callbackDataClassification_Value, deducedDataClassification_Value);
            } catch (Exception e) {
                jcLog("PR-2410: setClassificationFromCallbackIfValid #6");
                final JsonNode defaultDataClassification_Value;
                try {
                    jcLog("PR-2410: setClassificationFromCallbackIfValid #7");
                    Optional<CaseDetails> defaultCaseDetails =
                        classifiedGetCaseOperation.execute(caseDetails.getReferenceAsString());
                    jcLog("PR-2410: setClassificationFromCallbackIfValid #8");
                    defaultDataClassification_Value =
                        JacksonUtils.convertValueJsonNode(defaultCaseDetails.get().getDataClassification());
                } catch (Exception e2) {
                    jcLog("PR-2410: setClassificationFromCallbackIfValid #9");
                    throw new ValidationException(VALIDATION_ERR_MSG);
                }
                jcLog("PR-2410: setClassificationFromCallbackIfValid #10");
                validateObject(callbackDataClassification_Value, defaultDataClassification_Value);
            }

            jcLog("PR-2410: setClassificationFromCallbackIfValid #11  [OK]");
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
