package uk.gov.hmcts.ccd.data.casedetails;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Named
@Singleton
public class CaseAuditEventMapper {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final TypeReference STRING_JSON_MAP_TYPE = new TypeReference<HashMap<String, JsonNode>>() {
    };

    public AuditEvent entityToModel(final CaseAuditEventEntity caseAuditEventEntity) {
        final AuditEvent auditEvent = new AuditEvent();
        auditEvent.setId(caseAuditEventEntity.getId());
        auditEvent.setCaseDataId(String.valueOf(caseAuditEventEntity.getCaseDataId()));
        auditEvent.setCaseTypeId(caseAuditEventEntity.getCaseTypeId());
        auditEvent.setCaseTypeVersion(caseAuditEventEntity.getCaseTypeVersion());
        auditEvent.setCreatedDate(caseAuditEventEntity.getCreatedDate());
        auditEvent.setStateId(caseAuditEventEntity.getStateId());
        auditEvent.setStateName(caseAuditEventEntity.getStateName());
        auditEvent.setUserId(caseAuditEventEntity.getUserId());
        auditEvent.setUserFirstName(caseAuditEventEntity.getUserFirstName());
        auditEvent.setUserLastName(caseAuditEventEntity.getUserLastName());
        auditEvent.setSecurityClassification(caseAuditEventEntity.getSecurityClassification());
        if (caseAuditEventEntity.getData() != null) {
            auditEvent.setData(mapper.convertValue(caseAuditEventEntity.getData(), STRING_JSON_MAP_TYPE));
            auditEvent.setDataClassification(mapper.convertValue(caseAuditEventEntity.getDataClassification(), STRING_JSON_MAP_TYPE));
        }
        auditEvent.setEventId(caseAuditEventEntity.getEventId());
        auditEvent.setEventName(caseAuditEventEntity.getEventName());
        auditEvent.setSummary(caseAuditEventEntity.getSummary());
        auditEvent.setDescription(caseAuditEventEntity.getDescription());
        return auditEvent;
    }

    public CaseAuditEventEntity modelToEntity(final AuditEvent auditEvent) {
        final CaseAuditEventEntity newCaseAuditEventEntity = new CaseAuditEventEntity();
        newCaseAuditEventEntity.setId(auditEvent.getId());
        newCaseAuditEventEntity.setCaseDataId(Long.valueOf(auditEvent.getCaseDataId()));
        newCaseAuditEventEntity.setCaseTypeId(auditEvent.getCaseTypeId());
        newCaseAuditEventEntity.setCaseTypeVersion(auditEvent.getCaseTypeVersion());
        newCaseAuditEventEntity.setCreatedDate(auditEvent.getCreatedDate());
        newCaseAuditEventEntity.setStateId(auditEvent.getStateId());
        newCaseAuditEventEntity.setStateName(auditEvent.getStateName());
        newCaseAuditEventEntity.setUserId(auditEvent.getUserId());
        newCaseAuditEventEntity.setUserLastName(auditEvent.getUserLastName());
        newCaseAuditEventEntity.setUserFirstName(auditEvent.getUserFirstName());
        newCaseAuditEventEntity.setSecurityClassification(auditEvent.getSecurityClassification());
        if (auditEvent.getData() == null) {
            newCaseAuditEventEntity.setData(mapper.createObjectNode());
            newCaseAuditEventEntity.setDataClassification(mapper.createObjectNode());
        } else {
            newCaseAuditEventEntity.setData(mapper.convertValue(auditEvent.getData(), JsonNode.class));
            newCaseAuditEventEntity.setDataClassification(mapper.convertValue(auditEvent.getDataClassification(), JsonNode.class));
        }
        newCaseAuditEventEntity.setEventId(auditEvent.getEventId());
        newCaseAuditEventEntity.setEventName(auditEvent.getEventName());
        newCaseAuditEventEntity.setSummary(auditEvent.getSummary());
        newCaseAuditEventEntity.setDescription(auditEvent.getDescription());
        return newCaseAuditEventEntity;
    }

    public List<AuditEvent> entityToModel(final List<CaseAuditEventEntity> caseEventEntities) {
        return caseEventEntities.stream()
            .map(this::entityToModel)
            .collect(Collectors.toList());
    }
}
