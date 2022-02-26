package uk.gov.hmcts.ccd.domain.service.casedeletion;

import lombok.NonNull;
import uk.gov.hmcts.ccd.data.caseaccess.CaseLinkEntity;
import uk.gov.hmcts.ccd.domain.model.casedeletion.CaseLink;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Named;

@Named
public class CaseLinkMapper {

    public CaseLink entityToModel(@NonNull final CaseLinkEntity caseLinkEntity) {
        return CaseLink.builder()
            .caseId(caseLinkEntity.getCaseLinkPrimaryKey().getCaseId())
            .linkedCaseId(caseLinkEntity.getCaseLinkPrimaryKey().getLinkedCaseId())
            .caseTypeId(caseLinkEntity.getCaseTypeId())
            .build();
    }

    public List<CaseLink> entitiesToModels(@NonNull final List<CaseLinkEntity> caseLinkEntities) {
        return caseLinkEntities.stream()
            .map(this::entityToModel)
            .collect(Collectors.toList());
    }

    public CaseLinkEntity modelToEntity(@NonNull final CaseLink caseLink) {
        return new CaseLinkEntity(
            caseLink.getCaseId(),
            caseLink.getLinkedCaseId(),
            caseLink.getCaseTypeId());
    }
}

