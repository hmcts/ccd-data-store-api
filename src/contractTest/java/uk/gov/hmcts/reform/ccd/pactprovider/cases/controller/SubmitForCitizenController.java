package uk.gov.hmcts.reform.ccd.pactprovider.cases.controller;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.createcase.CreateCaseOperation;

import java.time.LocalDate;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
public class SubmitForCitizenController {

    private static final int SECURITY_LEVEL = 23;
    private static final String CALLBACK_RESPONSE_STATUS = "Success";

    private final CreateCaseOperation createCaseOperation;

    public SubmitForCitizenController(CreateCaseOperation createCaseOperation) {
        this.createCaseOperation = createCaseOperation;
    }

    @PostMapping(
        path = "/citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases",
        produces = APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> submitForCitizen(
        @PathVariable("uid") String userId,
        @PathVariable("jid") String jurisdictionId,
        @PathVariable("ctid") String caseTypeId,
        @RequestParam(value = "ignore-warning", required = false, defaultValue = "false") Boolean ignoreWarning,
        @RequestBody(required = false) CaseDataContent content
    ) {
        CaseDetails caseDetails = createCaseOperation.createCaseDetails(caseTypeId, content, ignoreWarning);

        if (content != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(caseDetails);
        }

        return ResponseEntity.ok(new LegacySubmitForCitizenResponse(
            SECURITY_LEVEL,
            caseDetails.getCreatedDate().toLocalDate(),
            CALLBACK_RESPONSE_STATUS,
            caseDetails.getCaseTypeId(),
            caseDetails.getState(),
            caseDetails.getLastModified().toLocalDate()
        ));
    }

    public record LegacySubmitForCitizenResponse(
        int securityLevel,
        @JsonFormat(pattern = "dd/MM/yyyy") LocalDate createdDate,
        String callbackResponseStatus,
        @JsonProperty("case_type") String caseType,
        String state,
        @JsonFormat(pattern = "dd/MM/yyyy") LocalDate lastModified
    ) {
    }
}
