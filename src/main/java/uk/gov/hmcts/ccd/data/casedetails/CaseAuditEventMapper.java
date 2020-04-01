package uk.gov.hmcts.ccd.data.casedetails;

import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.SignificantItemEntity;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItem;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItemType;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.config.JacksonUtils.MAPPER;

@Named
@Singleton
public class CaseAuditEventMapper {

    public AuditEvent entityToModel(final CaseAuditEventEntity caseAuditEventEntity) {
        final AuditEvent auditEvent = new AuditEvent();
        final SignificantItem significantItem = new SignificantItem();
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
            auditEvent.setData(JacksonUtils.convertValue(caseAuditEventEntity.getData()));
            auditEvent.setDataClassification(JacksonUtils.convertValue(caseAuditEventEntity.getDataClassification()));
        }
        auditEvent.setEventId(caseAuditEventEntity.getEventId());
        auditEvent.setEventName(caseAuditEventEntity.getEventName());
        auditEvent.setSummary(caseAuditEventEntity.getSummary());
        auditEvent.setDescription(caseAuditEventEntity.getDescription());
        if (caseAuditEventEntity.getSignificantItemEntity() != null) {
            significantItem.setDescription(caseAuditEventEntity.getSignificantItemEntity().getDescription());
            significantItem.setType(caseAuditEventEntity.getSignificantItemEntity().getType().name());
            significantItem.setUrl(caseAuditEventEntity.getSignificantItemEntity().getUrl());
            auditEvent.setSignificantItem(significantItem);
        }
        return auditEvent;
    }

    public CaseAuditEventEntity modelToEntity(final AuditEvent auditEvent) {
        final CaseAuditEventEntity newCaseAuditEventEntity = new CaseAuditEventEntity();
        final SignificantItemEntity significantItemEntity = new SignificantItemEntity();

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
            newCaseAuditEventEntity.setData(MAPPER.createObjectNode());
            newCaseAuditEventEntity.setDataClassification(MAPPER.createObjectNode());
        } else {
            newCaseAuditEventEntity.setData(JacksonUtils.convertValueJsonNode(auditEvent.getData()));
            newCaseAuditEventEntity.setDataClassification(JacksonUtils.convertValueJsonNode(auditEvent.getDataClassification()));
        }
        newCaseAuditEventEntity.setEventId(auditEvent.getEventId());
        newCaseAuditEventEntity.setEventName(auditEvent.getEventName());
        newCaseAuditEventEntity.setSummary(auditEvent.getSummary());
        newCaseAuditEventEntity.setDescription(auditEvent.getDescription());
        if (auditEvent.getSignificantItem() != null) {
            significantItemEntity.setType(SignificantItemType.valueOf(auditEvent.getSignificantItem().getType()));
            significantItemEntity.setDescription(auditEvent.getSignificantItem().getDescription());
            significantItemEntity.setCaseEvent(newCaseAuditEventEntity);
            significantItemEntity.setUrl(auditEvent.getSignificantItem().getUrl());
            newCaseAuditEventEntity.setSignificantItemEntity(significantItemEntity);
        }
        return newCaseAuditEventEntity;
    }

    public List<AuditEvent> entityToModel(final List<CaseAuditEventEntity> caseEventEntities) {
        return caseEventEntities.stream()
            .map(this::entityToModel)
            .collect(Collectors.toList());
    }
}
