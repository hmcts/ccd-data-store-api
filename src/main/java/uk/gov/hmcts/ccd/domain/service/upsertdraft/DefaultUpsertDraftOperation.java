package uk.gov.hmcts.ccd.domain.service.upsertdraft;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.DefaultDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDraftRequest;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.draft.UpdateCaseDraftRequest;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.types.sanitiser.CaseSanitiser;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import javax.inject.Inject;

import java.util.HashMap;

import static uk.gov.hmcts.ccd.domain.model.draft.CaseDraftBuilder.aCaseDraft;
import static uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDraftBuilder.aCreateCaseDraft;
import static uk.gov.hmcts.ccd.domain.model.draft.DraftResponseBuilder.aDraftResponse;
import static uk.gov.hmcts.ccd.domain.model.draft.UpdateCaseDraftBuilder.anUpdateCaseDraft;

@Service
@Qualifier("default")
public class DefaultUpsertDraftOperation implements UpsertDraftOperation {
    private static final HashMap<String, JsonNode> EMPTY_DATA_CLASSIFICATION = Maps.newHashMap();

    private final DraftGateway draftGateway;
    private final ApplicationParams applicationParams;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseSanitiser caseSanitiser;
    private final CaseDataService caseDataService;

    protected static final String CASE_DATA_CONTENT = "CaseDataContent";

    @Inject
    public DefaultUpsertDraftOperation(@Qualifier(DefaultDraftGateway.QUALIFIER) final DraftGateway draftGateway,
                                       @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                       final CaseDataService caseDataService,
                                       final CaseSanitiser caseSanitiser,

                                       ApplicationParams applicationParams) {
        this.draftGateway = draftGateway;
        this.applicationParams = applicationParams;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseSanitiser = caseSanitiser;
        this.caseDataService = caseDataService;
    }

    @Override
    public DraftResponse executeSave(final String uid,
                             final String jurisdictionId,
                             final String caseTypeId,
                             final String eventTriggerId,
                             final CaseDataContent caseDataContent) {
        return aDraftResponse()
            .withId(draftGateway.save(buildCreateCaseDraft(uid, jurisdictionId, caseTypeId, eventTriggerId, caseDataContent)))
            .build();
    }

    @Override
    public DraftResponse executeUpdate(final String uid,
                               final String jurisdictionId,
                               final String caseTypeId,
                               final String eventTriggerId,
                               final String draftId,
                               final CaseDataContent caseDataContent) {
        return draftGateway.update(buildUpdateCaseDraft(uid, jurisdictionId, caseTypeId, eventTriggerId, caseDataContent),
                                   draftId);
    }

    private CreateCaseDraftRequest buildCreateCaseDraft(String uid, String jurisdictionId, String caseTypeId, String eventTriggerId, CaseDataContent caseDataContent) {
        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseType == null) {
            throw new ValidationException("Cannot find case type definition for " + caseTypeId);
        }
        caseDataContent.setData(caseSanitiser.sanitise(caseType, caseDataContent.getData()));
        caseDataContent.setSecurityClassification(caseType.getSecurityClassification().name());
        caseDataContent.setDataClassification(caseDataService.getDefaultSecurityClassifications(caseType, caseDataContent.getData(), EMPTY_DATA_CLASSIFICATION));
        return aCreateCaseDraft()
            .withDocument(aCaseDraft()
                              .withUserId(uid)
                              .withJurisdictionId(jurisdictionId)
                              .withCaseTypeId(caseTypeId)
                              .withEventTriggerId(eventTriggerId)
                              .withCaseDataContent(caseDataContent)
                              .build())
            .withType(CASE_DATA_CONTENT)
            .withMaxStaleDays(applicationParams.getDraftMaxStaleDays())
            .build();
    }

    private UpdateCaseDraftRequest buildUpdateCaseDraft(String uid, String jurisdictionId, String caseTypeId, String eventTriggerId, CaseDataContent caseDataContent) {
        //It seems we're discarding the existing values so I'm discarding the existing security classification and data classification
        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseType == null) {
            throw new ValidationException("Cannot find case type definition for " + caseTypeId);
        }
        caseDataContent.setData(caseSanitiser.sanitise(caseType, caseDataContent.getData()));
        caseDataContent.setSecurityClassification(caseType.getSecurityClassification().name());
        caseDataContent.setDataClassification(caseDataService.getDefaultSecurityClassifications(caseType, caseDataContent.getData(), EMPTY_DATA_CLASSIFICATION));
        return anUpdateCaseDraft()
            .withDocument(aCaseDraft()
                              .withUserId(uid)
                              .withJurisdictionId(jurisdictionId)
                              .withCaseTypeId(caseTypeId)
                              .withEventTriggerId(eventTriggerId)
                              .withCaseDataContent(caseDataContent)
                              .build())
            .withType(CASE_DATA_CONTENT)
            .build();
    }

}
