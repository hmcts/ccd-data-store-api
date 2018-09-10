package uk.gov.hmcts.ccd.data.casedetails;

import uk.gov.hmcts.ccd.domain.model.definition.CaseType;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.stream.Collectors;

@Named
@Singleton
public class CaseTypeMapper {
    CaseStateMapper caseStateMapper;

    public CaseTypeMapper(CaseStateMapper caseStateMapper) {
        this.caseStateMapper = caseStateMapper;
    }

    public CaseType toResponse(CaseType caseType) {
        CaseType result = new CaseType();
        result.setId(caseType.getId());
        result.setName(caseType.getName());
        result.setDescription(caseType.getDescription());
        result.setStates(caseType.getStates().stream().map(caseStateMapper::toResponse).collect(Collectors.toList()));
        return result;
    }
}
