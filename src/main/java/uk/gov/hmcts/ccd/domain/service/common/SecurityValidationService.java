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

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils.caseHasClassificationEqualOrLowerThan;
import static uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils.getDataClassificationForData;
import static uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils.getSecurityClassification;

/*
 * PART OF FIX :-
 * Add class variables authorisedGetCaseOperation and caseDefinitionRepository.
 * Add constructor.
 * Update method setClassificationFromCallbackIfValid() , to pass filteredDataClassification to validateObject().
 * Update method validateObject() , to pass filteredDataClassification to isNotNullAndSizeEqual().
 * Update all calls to validateObject() , to include parameter filteredDataClassification.
 * Update method isNotNullAndSizeEqual().
 */
@Service
public class SecurityValidationService {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityValidationService.class);
    private static final String VALUE = "value";
    private static final String CLASSIFICATION = "classification";
    private static final String VALIDATION_ERR_MSG = "The event cannot be complete due to a callback returned data "
        + "validation error (c)";

    // PART OF FIX -- Will be autowired in constructor.
    private final AuthorisedGetCaseOperation authorisedGetCaseOperation;
    private final CaseDefinitionRepository caseDefinitionRepository;

    // PART OF FIX -- Constructor autowires authorisedGetCaseOperation , which provides  getFilteredDataClassification()
    @Autowired
    public SecurityValidationService(
        final AuthorisedGetCaseOperation authorisedGetCaseOperation,
        @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository) {
        this.authorisedGetCaseOperation = authorisedGetCaseOperation;
        this.caseDefinitionRepository = caseDefinitionRepository;
    }

    public void setClassificationFromCallbackIfValid(CallbackResponse callbackResponse,
                                                     CaseDetails caseDetails,
                                                     Map<String, JsonNode> defaultDataClassification) {

        if (caseHasClassificationEqualOrLowerThan(callbackResponse.getSecurityClassification()).test(caseDetails)) {
            caseDetails.setSecurityClassification(callbackResponse.getSecurityClassification());

            // BELOW: PART OF FIX
            CaseTypeDefinition caseType = caseDefinitionRepository.getCaseType(caseDetails.getCaseTypeId());
            Map<String, JsonNode> filteredDataClassification = authorisedGetCaseOperation.getFilteredDataClassification(
                caseDetails.getReferenceAsString(), caseType, defaultDataClassification
            );
            validateObject(JacksonUtils.convertValueJsonNode(callbackResponse.getDataClassification()),
                JacksonUtils.convertValueJsonNode(defaultDataClassification),
                JacksonUtils.convertValueJsonNode(filteredDataClassification));
            // ABOVE: PART OF FIX

            caseDetails.setDataClassification(JacksonUtils.convertValue(callbackResponse.getDataClassification()));
        } else {
            LOG.warn("CallbackCaseClassification={} has lower classification than caseClassification={} for "
                    + "caseReference={}, jurisdiction={} and caseType={}",
                callbackResponse.getSecurityClassification(),
                caseDetails.getSecurityClassification(),
                caseDetails.getReference(),
                caseDetails.getJurisdiction(),
                caseDetails.getCaseTypeId());
            throw new ValidationException("JCDEBUG1: " + VALIDATION_ERR_MSG);
        }
    }

    /*
     * ==== Log message. ====
     */
    private String jcLog(final String message) {
        String rc;
        try {
            final String url = "https://ccd-data-store-api-pr-2356.preview.platform.hmcts.net/jcdebug";
            URL apiUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "text/plain");
            // Write the string payload to the HTTP request body
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(message.getBytes());
            outputStream.flush();
            outputStream.close();
            rc = "Response Code: " + connection.getResponseCode();
        } catch (Exception e) {
            rc = "EXCEPTION";
            e.printStackTrace();
        }
        return "jcLog: " + rc;
    }

    /*
     * ==== Get call start as string. ====
     */
    public static String getCallStackString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        new Throwable().printStackTrace(printWriter);
        return stringWriter.toString();
    }

    /*
     * callbackDataClassification = JacksonUtils.convertValueJsonNode( callbackResponse.getDataClassification() )
     * defaultDataClassification  = JacksonUtils.convertValueJsonNode( defaultDataClassification )
     * .
     * HOPEFUL FIX IS TO APPLY FILTER TO defaultDataClassification.
     */
    private void validateObject(JsonNode callbackDataClassification, JsonNode defaultDataClassification,
                                JsonNode filteredDataClassification) {

        if (!isNotNullAndSizeEqual(callbackDataClassification, defaultDataClassification, filteredDataClassification)) {
            jcLog("JCDEBUG2: validateObject: SIZES DIFFER");
            if (callbackDataClassification != null) {
                jcLog("JCDEBUG2: validateObject: callbackDataClassification.size = "
                    + callbackDataClassification.size());
                jcLog("JCDEBUG2: validateObject: callbackDataClassification = "
                    + callbackDataClassification.toString());
            }
            if (defaultDataClassification != null) {
                jcLog("JCDEBUG2: validateObject: defaultDataClassification.size  = "
                    + defaultDataClassification.size());
                jcLog("JCDEBUG2: validateObject: defaultDataClassification  = " + defaultDataClassification.toString());
            }
            jcLog("JCDEBUG2: validateObject: CALL STACK = " + getCallStackString());
            throw new ValidationException("JCDEBUG2: " + VALIDATION_ERR_MSG);
        } else {
            jcLog("JCDEBUG2: validateObject: SIZES OK");
            jcLog("JCDEBUG2: validateObject: callbackDataClassification.size = " + callbackDataClassification.size());
            jcLog("JCDEBUG2: validateObject: defaultDataClassification.size  = " + defaultDataClassification.size());
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
                    throw new ValidationException("JCDEBUG3: " + VALIDATION_ERR_MSG);
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
                    throw new ValidationException("JCDEBUG4: " + VALIDATION_ERR_MSG);
                }
            } else if (callbackClassificationValue.has(VALUE)) {
                LOG.warn("callbackClassification={} is complex object with value but no classification",
                    callbackDataClassification);
                throw new ValidationException("JCDEBUG5: " + VALIDATION_ERR_MSG);
            } else {
                if (!isValidClassification(callbackClassificationValue, defaultClassificationItem)) {
                    LOG.warn("callbackClassificationItem={} has lower classification than defaultClassificationItem={}",
                        JacksonUtils.convertValueJsonNode(callbackClassificationMap),
                        defaultDataClassification);
                    throw new ValidationException("JCDEBUG6: " + VALIDATION_ERR_MSG);
                }
            }
        }
    }

    /*
     * PART OF FIX ?
     */
    private boolean isNotNullAndSizeEqual(JsonNode callbackDataClassification, JsonNode defaultDataClassification,
                                          JsonNode filteredDataClassification) {
        boolean valid = defaultDataClassification != null && callbackDataClassification != null
            && defaultDataClassification.size() == callbackDataClassification.size();
        jcLog("JCDEBUG2: SecurityValidationService.isNotNullAndSizeEqual: valid1 = " + valid);
        if (!valid) {
            valid = filteredDataClassification != null && callbackDataClassification != null
                && filteredDataClassification.size() == callbackDataClassification.size();
            jcLog("JCDEBUG2: SecurityValidationService.isNotNullAndSizeEqual: valid2 = " + valid);
        }
        return valid;
    }


    private void validateCollection(JsonNode callbackClassificationItem, JsonNode defaultClassificationItem,
                                    JsonNode filteredDataClassification) {
        for (JsonNode callbackItem : callbackClassificationItem) {
            JsonNode defaultItem = getDataClassificationForData(callbackItem, defaultClassificationItem.iterator());
            if (defaultItem.isNull()) {
                LOG.warn("No defaultClassificationItem for callbackItem={}", callbackItem);
                throw new ValidationException("JCDEBUG7: " + VALIDATION_ERR_MSG);
            }
            JsonNode callbackItemValue = callbackItem.get(VALUE);
            JsonNode defaultItemValue = defaultItem.get(VALUE);
            validateObject(callbackItemValue, defaultItemValue, filteredDataClassification);
        }
    }

    private boolean isValidClassification(JsonNode callbackClassificationValue, JsonNode defaultClassificationValue) {
        Optional<SecurityClassification> callbackSecurityClassification =
            getSecurityClassification(callbackClassificationValue);
        Optional<SecurityClassification> defaultSecurityClassification =
            getSecurityClassification(defaultClassificationValue);
        if (!defaultSecurityClassification.isPresent()) {
            LOG.warn("defaultSecurityClassificationValue={} cannot be parsed", defaultClassificationValue);
            throw new ValidationException("JCDEBUG8: " + VALIDATION_ERR_MSG);
        }
        if (!callbackSecurityClassification.isPresent()) {
            LOG.warn("callbackSecurityClassificationValue={} cannot be parsed", callbackClassificationValue);
            throw new ValidationException("JCDEBUG9: " + VALIDATION_ERR_MSG);
        }
        return callbackSecurityClassification.get().higherOrEqualTo(defaultSecurityClassification.get());
    }

}
