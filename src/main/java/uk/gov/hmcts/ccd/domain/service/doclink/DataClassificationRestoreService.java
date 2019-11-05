package uk.gov.hmcts.ccd.domain.service.doclink;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;

import java.util.HashMap;
import java.util.Map;

@Service
public class DataClassificationRestoreService {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final TypeReference STRING_JSON_MAP_TYPE = new TypeReference<HashMap<String, JsonNode>>() {
    };

    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseDataService caseDataService;

    @Autowired
    public DataClassificationRestoreService(@Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                                CaseDefinitionRepository caseDefinitionRepository, CaseDataService caseDataService) {
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseDataService = caseDataService;
    }

    public void deduceAndUpdateDataClassification(CaseDetailsEntity caseDetailsEntity) {
        final CaseType caseType = caseDefinitionRepository.getCaseType(caseDetailsEntity.getCaseType());
        Map<String, JsonNode> caseData = mapper.convertValue(caseDetailsEntity.getData(), STRING_JSON_MAP_TYPE);
        Map<String, JsonNode> currentClassification = mapper.convertValue(caseDetailsEntity.getDataClassification(), STRING_JSON_MAP_TYPE);

        Map<String, JsonNode> updatedClassification = caseDataService.getDefaultSecurityClassifications(caseType, caseData, currentClassification);

        caseDetailsEntity.setDataClassification(mapper.convertValue(updatedClassification, JsonNode.class));
    }
}
