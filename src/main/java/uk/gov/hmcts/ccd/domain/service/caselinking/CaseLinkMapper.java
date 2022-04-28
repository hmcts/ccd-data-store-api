package uk.gov.hmcts.ccd.domain.service.caselinking;

import lombok.NonNull;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.caselinking.CaseLinkEntity;
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLink;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CaseLinkMapper {

    public CaseLink entityToModel(@NonNull final CaseLinkEntity caseLinkEntity) {
        return CaseLink.builder()
            .caseId(caseLinkEntity.getCaseLinkPrimaryKey().getCaseId())
            .linkedCaseId(caseLinkEntity.getCaseLinkPrimaryKey().getLinkedCaseId())
            .caseTypeId(caseLinkEntity.getCaseTypeId())
            .standardLink(caseLinkEntity.getStandardLink())
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
            caseLink.getCaseTypeId(),
            caseLink.getStandardLink());
    }

}
