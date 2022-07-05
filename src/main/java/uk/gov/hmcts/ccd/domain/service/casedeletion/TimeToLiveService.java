package uk.gov.hmcts.ccd.domain.service.casedeletion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.casedeletion.TTL;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static uk.gov.hmcts.ccd.domain.model.casedeletion.TTL.NO;
import static uk.gov.hmcts.ccd.domain.model.casedeletion.TTL.TTL_CASE_FIELD_ID;

@Slf4j
@Service
public class TimeToLiveService {

    protected static final String TIME_TO_LIVE_MODIFIED_ERROR_MESSAGE =
        "Time to live content has been modified by callback";
    protected static final String FAILED_TO_READ_TTL_FROM_CASE_DATA = "Failed to read TTL from case data";

    private final ObjectMapper objectMapper;
    private final ApplicationParams applicationParams;
    private final CaseDataService caseDataService;

    @Autowired
    public TimeToLiveService(@Qualifier("DefaultObjectMapper") ObjectMapper objectMapper,
                             ApplicationParams applicationParams,
                             CaseDataService caseDataService) {
        this.objectMapper = objectMapper;
        this.applicationParams = applicationParams;
        this.caseDataService = caseDataService;
    }

    public boolean isCaseTypeUsingTTL(@NonNull CaseTypeDefinition caseTypeDefinition) {
        return Optional.ofNullable(caseTypeDefinition.getCaseFieldDefinitions()).orElse(Collections.emptyList())
            .stream().anyMatch(caseFieldDefinition -> TTL_CASE_FIELD_ID.equals(caseFieldDefinition.getId()));
    }

    public Map<String, JsonNode> updateCaseDataClassificationWithTTL(Map<String, JsonNode> data,
                                                                     Map<String, JsonNode> dataClassification,
                                                                     CaseEventDefinition caseEventDefinition,
                                                                     CaseTypeDefinition caseTypeDefinition) {
        Map<String, JsonNode> clonedDataClassification = dataClassification;
        Integer ttlIncrement = caseEventDefinition.getTtlIncrement();

        // if TTL is in play then ensure data classification contains TTL data
        if (isCaseTypeUsingTTL(caseTypeDefinition) && (ttlIncrement != null)
            && data != null && data.get(TTL_CASE_FIELD_ID) != null) {

            // using just the TTL data ....
            Map<String, JsonNode> justTtlData = new HashMap<>();
            justTtlData.put(TTL_CASE_FIELD_ID, data.get(TTL_CASE_FIELD_ID));

            // ... generate just the TTL data classification
            Map<String, JsonNode> justTtlDataClassification = caseDataService.getDefaultSecurityClassifications(
                caseTypeDefinition,
                justTtlData,
                new HashMap<>()
            );

            // .. then clone current data classification and set the TTL classification
            clonedDataClassification = cloneOrNewJsonMap(dataClassification);
            clonedDataClassification.put(TTL_CASE_FIELD_ID, justTtlDataClassification.get(TTL_CASE_FIELD_ID));
        }

        return clonedDataClassification;
    }

    public Map<String, JsonNode> updateCaseDetailsWithTTL(Map<String, JsonNode> data,
                                                          CaseEventDefinition caseEventDefinition,
                                                          CaseTypeDefinition caseTypeDefinition) {
        Map<String, JsonNode> clonedData = data;
        Integer ttlIncrement = caseEventDefinition.getTtlIncrement();

        if (isCaseTypeUsingTTL(caseTypeDefinition) && (ttlIncrement != null)) {

            clonedData = cloneOrNewJsonMap(data);
            TTL timeToLive = null;

            // load existing TTL
            if (clonedData.get(TTL_CASE_FIELD_ID) != null) {
                timeToLive = getTTLFromJson(clonedData.get(TTL_CASE_FIELD_ID));
            }

            // if TTL still missing create one
            if (timeToLive == null) {
                timeToLive = TTL.builder().suspended(NO).build();
            }

            // set system TTL and write TTL field to cloned data
            timeToLive.setSystemTTL(LocalDate.now().plusDays(ttlIncrement));
            clonedData.put(TTL_CASE_FIELD_ID, objectMapper.valueToTree(timeToLive));
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

    private Map<String, JsonNode> cloneOrNewJsonMap(Map<String, JsonNode> jsonMap) {
        if (jsonMap != null) {
            // shallow clone
            return new HashMap<>(jsonMap);
        } else {
            return new HashMap<>();
        }
    }

}
