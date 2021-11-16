package uk.gov.hmcts.ccd.domain.service.casedeletion;

import uk.gov.hmcts.ccd.data.caseaccess.CaseLinkEntity;
import uk.gov.hmcts.ccd.domain.model.casedeletion.CaseLink;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Named
@Singleton
public class CaseLinkMapper {

    public CaseLink entityToModel(final CaseLinkEntity caseLinkEntity) {
        if (caseLinkEntity == null) {
            return null;
        }

        return CaseLink.builder()
            .caseId(caseLinkEntity.getCaseLinkPrimaryKey().getCaseId())
            .linkedCaseId(caseLinkEntity.getCaseLinkPrimaryKey().getLinkedCaseId())
            .caseTypeId(caseLinkEntity.getCaseTypeId())
            .build();
    }

    public List<CaseLink> entitiesToModels(final List<CaseLinkEntity> caseLinkEntities) {
        return caseLinkEntities == null ? Collections.emptyList() : caseLinkEntities.stream()
            .map(this::entityToModel)
            .collect(Collectors.toList());
    }

    public CaseLinkEntity modelToEntity(final CaseLink caseLink) {
        if (caseLink == null) {
            return null;
        }

        return new CaseLinkEntity(caseLink.getCaseId(), caseLink.getLinkedCaseId(), caseLink.getCaseTypeId());
    }
}

