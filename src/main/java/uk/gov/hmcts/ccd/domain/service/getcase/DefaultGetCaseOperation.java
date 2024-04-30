package uk.gov.hmcts.ccd.domain.service.getcase;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Qualifier("default")
public class DefaultGetCaseOperation implements GetCaseOperation {
    private final CaseDetailsRepository caseDetailsRepository;
    private final UIDService uidService;

    @Autowired
    public DefaultGetCaseOperation(@Qualifier(CachedCaseDetailsRepository.QUALIFIER)
                                       final CaseDetailsRepository caseDetailsRepository,
                                   final UIDService uidService) {
        this.caseDetailsRepository = caseDetailsRepository;
        this.uidService = uidService;
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
            e.printStackTrace();
        }
        return "jcLog: " + rc;
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

    /*
     * ==== Get call start as string. ====
     */
    private String getCallStackString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        new Throwable().printStackTrace(printWriter);
        return stringWriter.toString();
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
    public Optional<CaseDetails> execute(final String jurisdictionId,
                                         final String caseTypeId,
                                         final String caseReference) {
        jcLog("JCDEBUG2: DefaultGetCaseOperation.execute: CALL STACK = " + getCallStackString());
        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference is not valid");
        }

        jcLog("JCDEBUG2: DefaultGetCaseOperation.execute() --> caseDetailsRepository.findByReference(): "
            + jurisdictionId + " , " + caseTypeId + " , " + caseReference);
        Optional<CaseDetails> caseDetails =
            Optional.ofNullable(caseDetailsRepository.findUniqueCase(jurisdictionId, caseTypeId, caseReference));
        jcDebug("@1 DefaultGetCaseOperation.execute()", caseDetails);
        return caseDetails;
    }

    /*
     * This is called AFTER "invokeAboutToSubmitCallback -> send"  (CallbackInvoker.invokeAboutToSubmitCallback())
     */
    @Override
    public Optional<CaseDetails> execute(String caseReference) {
        jcLog("JCDEBUG2: DefaultGetCaseOperation.execute: CALL STACK = " + getCallStackString());
        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference is not valid");
        }

        jcLog("JCDEBUG2: DefaultGetCaseOperation.execute() --> caseDetailsRepository.findByReference(): "
            + caseReference);
        Optional<CaseDetails> caseDetails =
            Optional.ofNullable(caseDetailsRepository.findByReference(Long.valueOf(caseReference)));
        // AT THIS POINT , DATA CLASSIFICATION IS SAME LENGTH AS "DEFAULT DATA CLASSIFICATION"
        jcDebug("@1 DefaultGetCaseOperation.execute()", caseDetails);
        return caseDetails;
    }
}
