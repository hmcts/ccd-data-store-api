package uk.gov.hmcts.ccd.domain.service.getcase;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Qualifier(CreatorGetCaseOperation.QUALIFIER)
public class CreatorGetCaseOperation implements GetCaseOperation {

    public static final String QUALIFIER = "creator";

    private GetCaseOperation getCaseOperation;

    private CaseAccessService caseAccessService;

    private ApplicationParams applicationParams;

    public CreatorGetCaseOperation(@Qualifier("restricted") GetCaseOperation getCaseOperation,
                                   CaseAccessService caseAccessService,
                                   ApplicationParams applicationParams) {
        this.getCaseOperation = getCaseOperation;
        this.caseAccessService = caseAccessService;
        this.applicationParams = applicationParams;
    }

    /*
     * ==== Log message. ====
     */
    private String jcLog(final String message) {
        String rc;
        try {
            final String url = "https://ccd-data-store-api-pr-2356.preview.platform.hmcts.net/jcdebug";
            URL apiUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "text/plain");
            // Write the string payload to the HTTP request body
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(message.getBytes());
            outputStream.flush();
            outputStream.close();
            rc = "Response Code: " + connection.getResponseCode();
        } catch (Exception e) {
            rc = "EXCEPTION";
        }
        return "jcLog: " + rc;
    }

    /*
     * ==== Get call start as string. ====
     */
    private String getCallStackString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        new Throwable().printStackTrace(printWriter);
        return stringWriter.toString();
    }

    private void jcDebug(String message, final Optional<CaseDetails> caseDetails) {
        try {
            JsonNode callbackDataClassificationDebug =
                JacksonUtils.convertValueJsonNode(caseDetails.get().getDataClassification());
            jcLog("JCDEBUG2:      " + message + " callbackDataClassificationDebug.size = "
                + (callbackDataClassificationDebug == null ? "NULL" : callbackDataClassificationDebug.size()));
        } catch (NoSuchElementException e) {
            jcLog("JCDEBUG2:      " + message + " callbackDataClassificationDebug.size = NoSuchElementException");
        }
    }

    @Override
    public Optional<CaseDetails> execute(String jurisdictionId, String caseTypeId, String caseReference) {
        return this.getCaseOperation.execute(jurisdictionId, caseTypeId, caseReference)
            .flatMap(this::checkVisibility);
    }

    /*
     * CaseController.getCase underlying call stack :-
     * CreatorGetCaseOperation
     * RestrictedGetCaseOperation
     * AuthorisedGetCaseOperation  (and possibly DefaultGetCaseOperation)
     * ClassifiedGetCaseOperation
     * DefaultGetCaseOperation
     */
    @Override
    public Optional<CaseDetails> execute(String caseReference) {
        Optional<CaseDetails> caseDetails1 = this.getCaseOperation.execute(caseReference);
        jcDebug("CreatorGetCaseOperation.execute caseDetails1", caseDetails1);  // size = 287 (and 279)
        Optional<CaseDetails> caseDetails2 = caseDetails1.flatMap(this::checkVisibility);
        jcDebug("CreatorGetCaseOperation.execute caseDetails2", caseDetails2);  // size = 287 (and 279)
        return caseDetails2;
    }

    private Optional<CaseDetails> checkVisibility(CaseDetails caseDetails) {
        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            return Optional.of(caseDetails);
        }
        return this.caseAccessService.canUserAccess(caseDetails)
            ? Optional.of(caseDetails) : Optional.empty();
    }

}
