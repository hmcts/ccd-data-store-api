package uk.gov.hmcts.ccd.domain.service.casedeletion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
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

    protected static final String TIME_TO_LIVE_MODIFIED_ERROR_MESSAGE =
        "Time to live content has been modified by aboutToStart callback";
    protected static final String FAILED_TO_READ_TTL_FROM_CASE_DATA = "Failed to read TTL from case data";

    private ObjectMapper objectMapper;
    private ApplicationParams applicationParams;

    @Autowired
    public TimeToLiveService(@Qualifier("DefaultObjectMapper") ObjectMapper objectMapper,
                             ApplicationParams applicationParams) {
        this.objectMapper = objectMapper;
        this.applicationParams = applicationParams;
    }

    public Map<String, JsonNode> updateCaseDetailsWithTTL(Map<String, JsonNode> data,
                                                          CaseEventDefinition caseEventDefinition) {
        Map<String, JsonNode> clonedData = data;

        if (clonedData != null) {
            Integer ttlIncrement = caseEventDefinition.getTtlIncrement();
            clonedData = new HashMap<>(data);
            if (clonedData.get(TTL_CASE_FIELD_ID) != null && (ttlIncrement != null)) {
                TTL timeToLive = getTTLFromJson(clonedData.get(TTL_CASE_FIELD_ID));
                if (timeToLive != null) {
                    timeToLive.setSystemTTL(LocalDate.now().plusDays(ttlIncrement));
                    clonedData.put(TTL_CASE_FIELD_ID, objectMapper.valueToTree(timeToLive));
                }
            }
        }

        return clonedData;
    }

    public void verifyTTLContentNotChanged(Map<String, JsonNode> expected, Map<String, JsonNode> actual) {
        if (expected != null && actual != null) {
            TTL expectedTtl = getTTLFromJson(expected.get(TTL_CASE_FIELD_ID));
            TTL actualTtl = getTTLFromJson(actual.get(TTL_CASE_FIELD_ID));

            if (expectedTtl != null && !expectedTtl.equals(actualTtl)) {
                throw new BadRequestException(TIME_TO_LIVE_MODIFIED_ERROR_MESSAGE);
            }
        }
    }

    public void validateSuspensionChange(Map<String, JsonNode> beforeCallbackData,
                                          Map<String, JsonNode> currentDataInDatabase) {
        TTL beforeCallbackTTL = null;
        TTL currentTTLInDatabase = null;
        if (beforeCallbackData != null
            && beforeCallbackData.get(TTL_CASE_FIELD_ID) != null
            && currentDataInDatabase.get(TTL_CASE_FIELD_ID) != null) {
            beforeCallbackTTL = getTTLFromJson(beforeCallbackData.get(TTL_CASE_FIELD_ID));
            currentTTLInDatabase = getTTLFromJson(currentDataInDatabase.get(TTL_CASE_FIELD_ID));
        }

        if (beforeCallbackTTL == null || currentTTLInDatabase == null) {
            return;
        }
        // check caseDetailsInDatabase (which is the current state of the fields) against the updatedCaseDetails when
        // checking if TTL.suspended has changed value
        LocalDate localDate = LocalDate.now().plusDays(applicationParams.getTtlGuard());
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

    public LocalDate getUpdatedResolvedTTL(Map<String, JsonNode> caseData) {
        LocalDate resolveTTL = null;
        TTL afterCallbackTTL = null;
        if (caseData.get(TTL_CASE_FIELD_ID) != null) {
            afterCallbackTTL = getTTLFromJson(caseData.get(TTL_CASE_FIELD_ID));
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

    private TTL getTTLFromJson(JsonNode ttlJsonNode) {
        if (ttlJsonNode != null) {
            try {
                return objectMapper.readValue(ttlJsonNode.toString(), TTL.class);
            } catch (JsonProcessingException e) {
                throw new ValidationException(FAILED_TO_READ_TTL_FROM_CASE_DATA);
            }
        }
        return null;
    }
}
