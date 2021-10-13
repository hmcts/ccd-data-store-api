package uk.gov.hmcts.ccd.domain.service.casedeletion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.casedeletion.TTL;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static uk.gov.hmcts.ccd.domain.model.casedeletion.TTL.TTL_CASE_FIELD_ID;

@Slf4j
@Service
public class TimeToLiveService {

    protected static final String TIME_TO_LIVE_MODIFIED_ERROR_MESSAGE =
        "Time to live content has been modified by aboutToStart callback";

    private ObjectMapper objectMapper;

    @Autowired
    public TimeToLiveService(@Qualifier("DefaultObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, JsonNode> updateCaseDetailsWithTTL(Map<String, JsonNode> data,
                                                          CaseEventDefinition caseEventDefinition) {
        Map<String, JsonNode> clonedData = data;

        if (clonedData != null) {
            Integer ttlIncrement = caseEventDefinition.getTtlIncrement();
            clonedData = new HashMap<>(data);
            if (clonedData.get(TTL_CASE_FIELD_ID) != null && (ttlIncrement != null)) {
                try {
                    TTL timeToLive = getTTLFromJson(clonedData.get(TTL_CASE_FIELD_ID));
                    if (timeToLive != null) {
                        timeToLive.setSystemTTL(LocalDate.now().plusDays(ttlIncrement));
                        clonedData.put(TTL_CASE_FIELD_ID, objectMapper.valueToTree(timeToLive));
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
                    throw new BadRequestException(TIME_TO_LIVE_MODIFIED_ERROR_MESSAGE);
                }
            } catch (JsonProcessingException e) {
                log.error("Failed to read TTL from case data");
            }
        }
    }

    private TTL getTTLFromJson(JsonNode ttlJsonNode) throws JsonProcessingException {
        if (ttlJsonNode != null) {
            return objectMapper.readValue(ttlJsonNode.toString(), TTL.class);
        }
        return null;
    }
}
