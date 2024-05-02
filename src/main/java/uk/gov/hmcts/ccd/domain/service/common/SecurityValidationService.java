package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.getcase.AuthorisedGetCaseOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;
import uk.gov.hmcts.ccd.endpoint.std.TestController;

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
    private static final String VALIDATION_ERR_MSG = "The event cannot be complete due to a callback returned data "
        + "validation error (c)";

    private final AuthorisedGetCaseOperation authorisedGetCaseOperation;
    private final CaseDefinitionRepository caseDefinitionRepository;

    @Autowired
    public SecurityValidationService(
        final AuthorisedGetCaseOperation authorisedGetCaseOperation,
        @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository) {
        this.authorisedGetCaseOperation = authorisedGetCaseOperation;
        this.caseDefinitionRepository = caseDefinitionRepository;
    }

    private static String jcLog(final String message) {
        return TestController.jcLog(message);
    }

    public void setClassificationFromCallbackIfValid(CallbackResponse callbackResponse,
                                                     CaseDetails caseDetails,
                                                     Map<String, JsonNode> defaultDataClassification) {
        jcLog("JCDEBUG3: SecurityValidationService.setClassificationFromCallbackIfValid #1 -->");
        if (caseHasClassificationEqualOrLowerThan(callbackResponse.getSecurityClassification()).test(caseDetails)) {
            jcLog("JCDEBUG3: SecurityValidationService.setClassificationFromCallbackIfValid #2");
            caseDetails.setSecurityClassification(callbackResponse.getSecurityClassification());

            jcLog("JCDEBUG3: SecurityValidationService.setClassificationFromCallbackIfValid #3");
            CaseTypeDefinition caseType = caseDefinitionRepository.getCaseType(caseDetails.getCaseTypeId());
            Map<String, JsonNode> filteredDataClassification = authorisedGetCaseOperation.getFilteredDataClassification(
                caseDetails.getReferenceAsString(), caseType, defaultDataClassification
            );
            jcLog(
                "JCDEBUG3: SecurityValidationService.setClassificationFromCallbackIfValid #4 (before validateObject)");
            validateObject(JacksonUtils.convertValueJsonNode(callbackResponse.getDataClassification()),
                JacksonUtils.convertValueJsonNode(defaultDataClassification),
                JacksonUtils.convertValueJsonNode(filteredDataClassification));

            jcLog("JCDEBUG3: SecurityValidationService.setClassificationFromCallbackIfValid #5 (after validateObject)");
            caseDetails.setDataClassification(JacksonUtils.convertValue(callbackResponse.getDataClassification()));

            jcLog("JCDEBUG3: SecurityValidationService.setClassificationFromCallbackIfValid #6 (OK)");
        } else {
            jcLog("JCDEBUG3: SecurityValidationService.setClassificationFromCallbackIfValid #7 *ERROR*");
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

    private void validateObject(JsonNode callbackDataClassification, JsonNode defaultDataClassification,
                                JsonNode filteredDataClassification) {
        jcLog("JCDEBUG3: SecurityValidationService.validateObject #1 -->");
        if (!isNotNullAndSizeEqual(callbackDataClassification, defaultDataClassification, filteredDataClassification)) {
            LOG.warn("callbackClassification={} and defaultClassification={} sizes differ", callbackDataClassification,
                defaultDataClassification);
            throw new ValidationException(VALIDATION_ERR_MSG);
        }

        jcLog("JCDEBUG3: SecurityValidationService.validateObject #2 (after size check)");
        Iterator<Map.Entry<String, JsonNode>> callbackDataClassificationIterator = callbackDataClassification.fields();
        while (callbackDataClassificationIterator.hasNext()) {
            Map.Entry<String, JsonNode> callbackClassificationMap = callbackDataClassificationIterator.next();
            String callbackClassificationKey = callbackClassificationMap.getKey();
            jcLog("JCDEBUG3: SecurityValidationService.validateObject #3 key = " + callbackClassificationKey);
            JsonNode callbackClassificationValue = callbackClassificationMap.getValue();
            JsonNode defaultClassificationItem = defaultDataClassification.get(callbackClassificationKey);
            JsonNode filteredClassificationItem = filteredDataClassification.get(callbackClassificationKey);
            if (callbackClassificationValue.has(CLASSIFICATION)) {
                // FAILING IN isValidClassification()
                if (!isValidClassification(callbackClassificationValue.get(CLASSIFICATION),
                    defaultClassificationItem.get(CLASSIFICATION), filteredClassificationItem.get(CLASSIFICATION))) {
                    LOG.warn("callbackClassificationItem={} has lower classification than defaultClassificationItem={}",
                        callbackClassificationValue,
                        defaultClassificationItem, filteredClassificationItem);
                    jcLog("JCDEBUG3: SecurityValidationService.validateObject #4 *EXCEPTION*");
                    throw new ValidationException(VALIDATION_ERR_MSG);
                }
                if (callbackClassificationValue.has(VALUE)) {
                    JsonNode defaultClassificationValue = defaultClassificationItem.get(VALUE);
                    JsonNode callbackClassificationItem = callbackClassificationValue.get(VALUE);
                    if (callbackClassificationItem.isObject()) {
                        validateObject(callbackClassificationItem, defaultClassificationValue,
                            filteredDataClassification);
                    } else {
                        validateCollection(callbackClassificationItem, defaultClassificationValue,
                            filteredDataClassification);
                    }
                } else {
                    LOG.warn("callbackClassification={} is complex object with classification but no value",
                        callbackDataClassification);
                    jcLog("JCDEBUG3: SecurityValidationService.validateObject #5 *EXCEPTION*");
                    throw new ValidationException(VALIDATION_ERR_MSG);
                }
            } else if (callbackClassificationValue.has(VALUE)) {
                LOG.warn("callbackClassification={} is complex object with value but no classification",
                    callbackDataClassification);
                jcLog("JCDEBUG3: SecurityValidationService.validateObject #6 *EXCEPTION*");
                throw new ValidationException(VALIDATION_ERR_MSG);
            } else {
                if (!isValidClassification(callbackClassificationValue, defaultClassificationItem,
                    null)) {
                    LOG.warn("callbackClassificationItem={} has lower classification than defaultClassificationItem={}",
                        JacksonUtils.convertValueJsonNode(callbackClassificationMap),
                        defaultDataClassification);
                    jcLog("JCDEBUG3: SecurityValidationService.validateObject #7 *EXCEPTION*");
                    throw new ValidationException(VALIDATION_ERR_MSG);
                }
            }
        }
        jcLog("JCDEBUG3: SecurityValidationService.validateObject #8 (OK)");
    }

    private boolean isNotNullAndSizeEqual(JsonNode callbackDataClassification, JsonNode defaultDataClassification,
                                          JsonNode filteredDataClassification) {
        boolean valid = defaultDataClassification != null && callbackDataClassification != null
            && defaultDataClassification.size() == callbackDataClassification.size();
        //jcLog("JCDEBUG3: SecurityValidationService.isNotNullAndSizeEqual: valid1 = " + valid);
        if (!valid) {
            valid = filteredDataClassification != null && callbackDataClassification != null
                && filteredDataClassification.size() == callbackDataClassification.size();
            //jcLog("JCDEBUG3: SecurityValidationService.isNotNullAndSizeEqual: valid2 = " + valid);
        }
        return valid;
    }


    private void validateCollection(JsonNode callbackClassificationItem, JsonNode defaultClassificationItem,
                                    JsonNode filteredDataClassification) {
        for (JsonNode callbackItem : callbackClassificationItem) {
            JsonNode defaultItem = getDataClassificationForData(callbackItem, defaultClassificationItem.iterator());
            if (defaultItem.isNull()) {
                LOG.warn("No defaultClassificationItem for callbackItem={}", callbackItem);
                throw new ValidationException(VALIDATION_ERR_MSG);
            }
            JsonNode callbackItemValue = callbackItem.get(VALUE);
            JsonNode defaultItemValue = defaultItem.get(VALUE);
            validateObject(callbackItemValue, defaultItemValue, filteredDataClassification);
        }
    }

    /*
     * QUESTION: Is rankFilteredSecurityClassification different from rankDefaultSecurityClassification ?
     */
    private boolean isValidClassification(JsonNode callbackClassificationValue, JsonNode defaultClassificationValue,
                                          JsonNode filteredClassificationValue) {
        jcLog("JCDEBUG3: SecurityValidationService.isValidClassification #1 -->");
        Optional<SecurityClassification> callbackSecurityClassification =
            getSecurityClassification(callbackClassificationValue);
        Optional<SecurityClassification> defaultSecurityClassification =
            getSecurityClassification(defaultClassificationValue);
        Optional<SecurityClassification> filteredSecurityClassification =
            getSecurityClassification(filteredClassificationValue);
        if (!defaultSecurityClassification.isPresent()) {
            LOG.warn("defaultSecurityClassificationValue={} cannot be parsed", defaultClassificationValue);
            throw new ValidationException(VALIDATION_ERR_MSG);
        }
        if (!callbackSecurityClassification.isPresent()) {
            LOG.warn("callbackSecurityClassificationValue={} cannot be parsed", callbackClassificationValue);
            throw new ValidationException(VALIDATION_ERR_MSG);
        }
        if (filteredSecurityClassification != null && filteredSecurityClassification.isPresent()) {
            int rank0 = filteredSecurityClassification.get().getRank();
            jcLog("JCDEBUG3: SecurityValidationService.isValidClassification #2 rankFilteredSecurityClassification = "
                + rank0);
        } else {
            jcLog("JCDEBUG3: SecurityValidationService.isValidClassification #2 rankFilteredSecurityClassification = "
                + "NULL");
        }
        int rank1 = callbackSecurityClassification.get().getRank();
        int rank2 = defaultSecurityClassification.get().getRank();
        boolean valid = callbackSecurityClassification.get().higherOrEqualTo(defaultSecurityClassification.get());
        jcLog("JCDEBUG3: SecurityValidationService.isValidClassification #3 rankCallbackSecurityClassification = "
            + rank1);
        jcLog("JCDEBUG3: SecurityValidationService.isValidClassification #4 rankDefaultSecurityClassification = "
            + rank2);
        jcLog("JCDEBUG3: SecurityValidationService.isValidClassification #5 VALID = " + valid);
        return valid;
    }

}
