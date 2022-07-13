package uk.gov.hmcts.ccd.domain.service.casedeletion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
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

@Service
public class TimeToLiveService {

    protected static final String TIME_TO_LIVE_MODIFIED_ERROR_MESSAGE =
        "Time to live content has been modified by callback";
    protected static final String TIME_TO_LIVE_GUARD_ERROR_MESSAGE =
        "Updating the TTL suspension or override values only allowed if the deletion will occur beyond the guard period.";
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
        Map<String, JsonNode> outputDataClassification = dataClassification;
        Integer ttlIncrement = caseEventDefinition.getTtlIncrement();

        // if TTL is in play then ensure data classification contains TTL data
        if (isCaseTypeUsingTTL(caseTypeDefinition) && (ttlIncrement != null) && isTtlCaseFieldPresent(data)) {

            // generate just the TTL data classification from just the TTL field data
            Map<String, JsonNode> justTtlDataClassification = caseDataService.getDefaultSecurityClassifications(
                caseTypeDefinition,
                Map.of(TTL_CASE_FIELD_ID, data.get(TTL_CASE_FIELD_ID)),
                new HashMap<>()
            );

            // .. then clone current data classification and set the TTL classification
            outputDataClassification = cloneOrNewJsonMap(dataClassification);
            outputDataClassification.put(TTL_CASE_FIELD_ID, justTtlDataClassification.get(TTL_CASE_FIELD_ID));
        }

        return outputDataClassification;
    }

    public Map<String, JsonNode> updateCaseDetailsWithTTL(Map<String, JsonNode> data,
                                                          CaseEventDefinition caseEventDefinition,
                                                          CaseTypeDefinition caseTypeDefinition) {
        Map<String, JsonNode> outputData = data;
        Integer ttlIncrement = caseEventDefinition.getTtlIncrement();

        if (isCaseTypeUsingTTL(caseTypeDefinition) && (ttlIncrement != null)) {

            outputData = cloneOrNewJsonMap(data);
            TTL timeToLive = null;

            // load existing TTL
            if (outputData.get(TTL_CASE_FIELD_ID) != null) {
                timeToLive = getTTLFromJson(outputData.get(TTL_CASE_FIELD_ID));
            }

            // if TTL still missing create one
            if (timeToLive == null) {
                timeToLive = TTL.builder().suspended(NO).build();
            }

            // set system TTL and write TTL field to cloned data
            timeToLive.setSystemTTL(LocalDate.now().plusDays(ttlIncrement));
            outputData.put(TTL_CASE_FIELD_ID, objectMapper.valueToTree(timeToLive));
        }

        return outputData;
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

    public void validateSuspensionChange(Map<String, JsonNode> updatedCaseData,
                                          Map<String, JsonNode> currentCaseData) {

        // the rule: "Unsetting a suspension can only be allowed if the deletion will occur beyond the guard period."

        TTL updatedTTL = null;
        TTL currentTTL = null;

        if (isTtlCaseFieldPresent(updatedCaseData) && isTtlCaseFieldPresent(currentCaseData)) {
            updatedTTL = getTTLFromJson(updatedCaseData.get(TTL_CASE_FIELD_ID));
            currentTTL = getTTLFromJson(currentCaseData.get(TTL_CASE_FIELD_ID));
        }

        if (updatedTTL == null || currentTTL == null) {
            // if `updatedTTL` is null then all is OK as no TTL in place, i.e. no deletion will occur.
            // if `currentTTL` is null then all is OK as no previous suspension to validate against.
            return;
        }

        // checking if `TTL.suspended` has changed value
        if (updatedTTL.isSuspended() != currentTTL.isSuspended()
            || overrideTTLIsChanged(currentTTL.getOverrideTTL(), updatedTTL.getOverrideTTL())) {
            LocalDate ttlGuardDate = LocalDate.now().plusDays(applicationParams.getTtlGuard());
            LocalDate resolvedTTL = getResolvedTTL(updatedTTL);

            // NB: if `resolvedTTL` is non-null then no suspension in place  ...
            //     ... so given we know `TTL.suspended` has changed value it must have been lifted

            // validate: Unsetting a suspension can only be allowed if the deletion will occur beyond the guard period
            if (resolvedTTL != null && resolvedTTL.isBefore(ttlGuardDate)) {
                throw new ValidationException(TIME_TO_LIVE_GUARD_ERROR_MESSAGE);
            }
        }
    }

    // check if overrideTTL has changed value
    private boolean overrideTTLIsChanged(LocalDate currentOverrideTTL, LocalDate updatedOverrideTTL) {
        if (currentOverrideTTL == null && updatedOverrideTTL == null
            || (currentOverrideTTL != null && updatedOverrideTTL != null
            && currentOverrideTTL.equals(updatedOverrideTTL))) {
            return false;
        }
        return true;
    }

    public LocalDate getUpdatedResolvedTTL(Map<String, JsonNode> caseData) {
        TTL ttl = null;

        if (isTtlCaseFieldPresent(caseData)) {
            ttl = getTTLFromJson(caseData.get(TTL_CASE_FIELD_ID));
        }

        return getResolvedTTL(ttl);
    }

    private LocalDate getResolvedTTL(TTL ttl) {
        LocalDate resolvedTTL = null; // default response

        if (ttl != null && (!ttl.isSuspended())) {
            resolvedTTL = ttl.getOverrideTTL() != null
                ? ttl.getOverrideTTL()
                : ttl.getSystemTTL();
        }

        return resolvedTTL;
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

    private boolean isTtlCaseFieldPresent(Map<String, JsonNode> data) {
        return data != null && data.get(TTL_CASE_FIELD_ID) != null;
    }

}
