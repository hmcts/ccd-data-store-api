package uk.gov.hmcts.ccd.domain.service.casedeletion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.casedeletion.TTL;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static uk.gov.hmcts.ccd.domain.model.casedeletion.TTL.TTL_CASE_FIELD_ID;

@Slf4j
@Service
public class TimeToLiveService {

    public static final Integer TTL_GUARD = 365;

    protected static final String TIME_TO_LIVE_MODIFIED_ERROR_MESSAGE =
        "Time to live content has been modified by aboutToStart callback";

    public Map<String, JsonNode> updateCaseDetailsWithTTL(Map<String, JsonNode> data,
                                                          CaseEventDefinition caseEventDefinition) {
        Map<String, JsonNode> clonedData = data;

        if (clonedData != null) {
            clonedData = new HashMap<>(data);
            Integer ttlIncrement = caseEventDefinition.getTtlIncrement();

            if (data.get(TTL_CASE_FIELD_ID) != null && (ttlIncrement != null)) {
                try {
                    TTL timeToLive = getTTLFromJson(data.get(TTL_CASE_FIELD_ID));
                    if (timeToLive != null) {
                        timeToLive.setSystemTTL(LocalDate.now().plusDays(ttlIncrement));
                        clonedData.put(TTL_CASE_FIELD_ID, JacksonUtils.MAPPER.valueToTree(timeToLive));
                    }
                } catch (JsonProcessingException e) {
                    log.error("Failed to read TTL from case data");
                }
            }
        }

        return clonedData;
    }

    public void verifyTTLContentNotChanged(Map<String, JsonNode> expected, Map<String, JsonNode> actual) {
        if (expected != null && actual != null) {
            try {
                TTL expectedTtl = getTTLFromJson(expected.get(TTL_CASE_FIELD_ID));
                TTL actualTtl = getTTLFromJson(actual.get(TTL_CASE_FIELD_ID));

                if (expectedTtl != null && !expectedTtl.equals(actualTtl)) {
                    // TODO - not sure what exception to throw here
                    throw new BadRequestException(TIME_TO_LIVE_MODIFIED_ERROR_MESSAGE);
                }
            } catch (JsonProcessingException e) {
                log.error("Failed to read TTL from case data");
            }
        }
    }

    public void validateSuspensionChange(Map<String, JsonNode> beforeCallbackData,
                                          Map<String, JsonNode> currentDataInDatabase) {
        TTL beforeCallbackTTL = null;
        TTL currentTTLInDatabase = null;
        try {
            beforeCallbackTTL = getTTLFromJson(beforeCallbackData.get(TTL_CASE_FIELD_ID));
            currentTTLInDatabase = getTTLFromJson(currentDataInDatabase.get(TTL_CASE_FIELD_ID));
        } catch (JsonProcessingException e) {
            log.error("Failed to read TTL from case data");
        }

        if (beforeCallbackTTL == null || currentTTLInDatabase == null) {
            return;
        }
        // check caseDetailsInDatabase (which is the current state of the fields) against the updatedCaseDetails when
        // checking if TTL.suspended has changed value
        LocalDate localDate = LocalDate.now().plusDays(TTL_GUARD);
        if (!beforeCallbackTTL.getSuspended().equalsIgnoreCase(currentTTLInDatabase.getSuspended())
            && (!beforeCallbackTTL.isSuspended()
            && (beforeCallbackTTL.getSystemTTL() != null
            && beforeCallbackTTL.getSystemTTL().isBefore(localDate)))
            && beforeCallbackTTL.getOverrideTTL() != null
            && beforeCallbackTTL.getOverrideTTL().isBefore(localDate)) {
            throw new ValidationException("Unsetting a suspension can only be allowed if"
                + " the deletion will occur beyond the guard period.");
        }
    }

    public LocalDate getUpdatedResolvedTTL(Map<String, JsonNode> afterCallbackData) {
        TTL afterCallbackTTL = null;
        LocalDate resolveTTL = null;
        try {
            afterCallbackTTL = getTTLFromJson(afterCallbackData.get(TTL_CASE_FIELD_ID));
        } catch (JsonProcessingException e) {
            log.error("Failed to read TTL from case data");
        }

        if (afterCallbackTTL != null) {
            if (afterCallbackTTL.isSuspended()) {
                resolveTTL = null;
            } else {
                resolveTTL = afterCallbackTTL.getOverrideTTL() != null
                    ? afterCallbackTTL.getOverrideTTL()
                    : afterCallbackTTL.getSystemTTL();
            }
        }
        return resolveTTL;
    }

    private TTL getTTLFromJson(JsonNode ttlJsonNode) throws JsonProcessingException {
        if (ttlJsonNode != null) {
            return JacksonUtils.MAPPER.readValue(ttlJsonNode.toString(), TTL.class);
        }
        return null;
    }
}
