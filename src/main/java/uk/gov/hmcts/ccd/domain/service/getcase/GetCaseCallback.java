package uk.gov.hmcts.ccd.domain.service.getcase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.callbacks.GetCaseCallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GetCaseCallback {
    private static final Logger LOG = LoggerFactory.getLogger(GetCaseCallback.class);

    private final CallbackInvoker callbackInvoker;

    public GetCaseCallback(CallbackInvoker callbackInvoker) {
        this.callbackInvoker = callbackInvoker;
    }

    public GetCaseCallbackResponse invoke(final CaseTypeDefinition caseTypeDefinition, final CaseDetails caseDetails,
                                          final List<CaseViewField> metadataFields) {
        try {
            ResponseEntity<GetCaseCallbackResponse> getCaseCallbackResponse = callbackInvoker
                .invokeGetCaseCallback(caseTypeDefinition, caseDetails);

            final GetCaseCallbackResponse response = getCaseCallbackResponse.getBody();
            validateNewMetadataFields(metadataFields, response);

            return response;
        } catch (Exception e) {
            LOG.error("CCD_CDI_CallbackGetCaseUrl: " + e.getMessage());
            throw new CallbackException(e.getMessage());
        }
    }

    private void validateNewMetadataFields(List<CaseViewField> metadataFields, GetCaseCallbackResponse response) {
        if (response == null) {
            throw new CallbackException("CCD_CDI_CallbackGetCaseUrl: response body not set");
        }
        Set<String> caseViewFieldIds = metadataFields.stream()
            .map(CaseViewField::getId).collect(Collectors.toSet());
        Set<String> callbackMetadataIds = response.getMetadataFields().stream()
            .map(CaseViewField::getId).collect(Collectors.toSet());
        callbackMetadataIds.retainAll(caseViewFieldIds);

        if (callbackMetadataIds.size() > 0) {
            throw new CallbackException("CCD_CDI_CallbackGetCaseUrl: "
                + "Following metadata ids are already present in the case_details" + callbackMetadataIds);
        }
    }
}
