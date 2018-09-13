package uk.gov.hmcts.ccd.data.casedetails;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Named
@Singleton
public class CaseDetailsMapper {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final TypeReference STRING_JSON_MAP_TYPE = new TypeReference<HashMap<String, JsonNode>>() {
    };

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
        caseDetails.setLastModified(caseDetailsEntity.getLastModified());
        caseDetails.setState(caseDetailsEntity.getState());
        caseDetails.setSecurityClassification(caseDetailsEntity.getSecurityClassification());
        if (caseDetailsEntity.getData() != null) {
            caseDetails.setData(mapper.convertValue(caseDetailsEntity.getData(), STRING_JSON_MAP_TYPE));
            caseDetails.setDataClassification(mapper.convertValue(caseDetailsEntity.getDataClassification(), STRING_JSON_MAP_TYPE));
        }
        return caseDetails;
    }

    public CaseDetailsEntity modelToEntity(final CaseDetails caseDetails) {
        if (caseDetails == null) {
            return null;
        }

        final CaseDetailsEntity newCaseDetailsEntity = new CaseDetailsEntity();
        newCaseDetailsEntity.setId(getLongId(caseDetails));
        newCaseDetailsEntity.setReference(caseDetails.getReference());
        newCaseDetailsEntity.setCreatedDate(caseDetails.getCreatedDate());
        newCaseDetailsEntity.setJurisdiction(caseDetails.getJurisdiction());
        newCaseDetailsEntity.setCaseType(caseDetails.getCaseTypeId());
        newCaseDetailsEntity.setState(caseDetails.getState());
        newCaseDetailsEntity.setSecurityClassification(caseDetails.getSecurityClassification());
        if (caseDetails.getData() == null) {
            newCaseDetailsEntity.setData(mapper.createObjectNode());
        } else {
            newCaseDetailsEntity.setData(mapper.convertValue(caseDetails.getData(), JsonNode.class));
            newCaseDetailsEntity.setDataClassification(mapper.convertValue(caseDetails.getDataClassification(), JsonNode.class));
        }
        return newCaseDetailsEntity;
    }

    private Long getLongId(CaseDetails caseDetails) {
        String id = caseDetails.getId();
        return id == null ? null : Long.valueOf(id);
    }

    public List<CaseDetails> entityToModel(final List<CaseDetailsEntity> caseDataEntities) {
        return caseDataEntities.stream()
            .map(this::entityToModel)
            .collect(Collectors.toList());
    }
}
