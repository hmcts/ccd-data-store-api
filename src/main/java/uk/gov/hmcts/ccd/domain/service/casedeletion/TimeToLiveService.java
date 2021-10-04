package uk.gov.hmcts.ccd.domain.service.casedeletion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
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
                    throw new BadRequestException("Time to live content has been modified");
                }
            } catch (JsonProcessingException e) {
                log.error("Failed to read TTL from case data");
            }
        }
    }

    private TTL getTTLFromJson(JsonNode ttlJsonNode) throws JsonProcessingException {
        if (ttlJsonNode != null) {
            return JacksonUtils.MAPPER.readValue(ttlJsonNode.toString(), TTL.class);
        }
        return null;
    }
}
