package uk.gov.hmcts.ccd.data.casedetails;

import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.config.JacksonUtils.MAPPER;

@Named
@Singleton
public class CaseDetailsMapper {

    public CaseDetails entityToModel(final CaseDetailsEntity caseDetailsEntity) {
        if (caseDetailsEntity == null) {
            return null;
        }

        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setReference(caseDetailsEntity.getReference());
        caseDetails.setId(String.valueOf(caseDetailsEntity.getId()));
        caseDetails.setCaseTypeId(caseDetailsEntity.getCaseType());
        caseDetails.setJurisdiction(caseDetailsEntity.getJurisdiction());
        caseDetails.setCreatedDate(caseDetailsEntity.getCreatedDate());
        caseDetails.setLastStateModifiedDate(caseDetailsEntity.getLastStateModifiedDate());
        caseDetails.setLastModified(caseDetailsEntity.getLastModified());
        caseDetails.setState(caseDetailsEntity.getState());
        caseDetails.setSecurityClassification(caseDetailsEntity.getSecurityClassification());
        caseDetails.setVersion(caseDetailsEntity.getVersion());
        if (caseDetailsEntity.getData() != null) {
            caseDetails.setData(JacksonUtils.convertValue(caseDetailsEntity.getData()));
            caseDetails.setDataClassification(JacksonUtils.convertValue(caseDetailsEntity.getDataClassification()));
        }
        if (caseDetailsEntity.getSupplementaryData() != null) {
            caseDetails.setSupplementaryData(JacksonUtils.convertValue(caseDetailsEntity.getSupplementaryData()));
        }
        return caseDetails;
    }

    public List<CaseDetails> entityToModel(final List<CaseDetailsEntity> caseDataEntities) {
        return caseDataEntities.stream()
            .map(this::entityToModel)
            .collect(Collectors.toList());
    }

    public CaseDetailsEntity modelToEntity(final CaseDetails caseDetails) {
        if (caseDetails == null) {
            return null;
        }

        final CaseDetailsEntity newCaseDetailsEntity = new CaseDetailsEntity();
        newCaseDetailsEntity.setId(getLongId(caseDetails));
        newCaseDetailsEntity.setReference(caseDetails.getReference());
        newCaseDetailsEntity.setCreatedDate(caseDetails.getCreatedDate());
        newCaseDetailsEntity.setLastStateModifiedDate(caseDetails.getLastStateModifiedDate());
        newCaseDetailsEntity.setJurisdiction(caseDetails.getJurisdiction());
        newCaseDetailsEntity.setCaseType(caseDetails.getCaseTypeId());
        newCaseDetailsEntity.setState(caseDetails.getState());
        newCaseDetailsEntity.setSecurityClassification(caseDetails.getSecurityClassification());
        newCaseDetailsEntity.setVersion(caseDetails.getVersion());
        if (caseDetails.getData() == null) {
            newCaseDetailsEntity.setData(MAPPER.createObjectNode());
        } else {
            newCaseDetailsEntity.setData(JacksonUtils.convertValueJsonNode(caseDetails.getData()));
            newCaseDetailsEntity.setDataClassification(
                JacksonUtils.convertValueJsonNode(caseDetails.getDataClassification()));
        }
        if (caseDetails.getSupplementaryData() != null) {
            newCaseDetailsEntity.setSupplementaryData(JacksonUtils.convertValueJsonNode(caseDetails
                    .getSupplementaryData()));
        }
        return newCaseDetailsEntity;
    }

    private Long getLongId(CaseDetails caseDetails) {
        String id = caseDetails.getId();
        return id == null ? null : Long.valueOf(id);
    }
}
