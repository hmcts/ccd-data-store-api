package uk.gov.hmcts.ccd.domain.service.getcase;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

/*
 * PART OF FIX :-
 * Add public method getFilteredDataClassification().
 */
@Service
@Qualifier("authorised")
public class AuthorisedGetCaseOperation implements GetCaseOperation {
    private final GetCaseOperation getCaseOperation;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final AccessControlService accessControlService;
    private final CaseDataAccessControl caseDataAccessControl;


    public AuthorisedGetCaseOperation(@Qualifier("classified") final GetCaseOperation getCaseOperation,
                                      @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                      final CaseDefinitionRepository caseDefinitionRepository,
                                      final AccessControlService accessControlService,
                                      CaseDataAccessControl caseDataAccessControl) {
        this.getCaseOperation = getCaseOperation;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.accessControlService = accessControlService;
        this.caseDataAccessControl = caseDataAccessControl;
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

        return this.execute(caseReference);
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
        jcDebug("AuthorisedGetCaseOperation.execute caseDetails1", caseDetails1);  // size = 288
        Optional<CaseDetails> caseDetails2 = caseDetails1.flatMap(caseDetails ->
            verifyReadAccess(getCaseType(caseDetails.getCaseTypeId()),
                getAccessProfiles(caseReference),
                caseDetails));
        jcDebug("AuthorisedGetCaseOperation.execute caseDetails2", caseDetails2);  // size = 287 (and 279)
        return caseDetails2;
    }

    private CaseTypeDefinition getCaseType(String caseTypeId) {
        return caseDefinitionRepository.getCaseType(caseTypeId);
    }


    private Set<AccessProfile> getAccessProfiles(String caseReference) {
        return caseDataAccessControl.generateAccessProfilesByCaseReference(caseReference);
    }

    private Optional<CaseDetails> verifyReadAccess(CaseTypeDefinition caseType, Set<AccessProfile> accessProfiles,
                                                   CaseDetails caseDetails) {

        if (caseType == null || caseDetails == null || CollectionUtils.isEmpty(accessProfiles)) {
            return Optional.empty();
        }

        if (!accessControlService.canAccessCaseTypeWithCriteria(caseType, accessProfiles, CAN_READ)
            || !accessControlService.canAccessCaseStateWithCriteria(caseDetails.getState(), caseType, accessProfiles,
            CAN_READ)) {
            return Optional.empty();
        }

        caseDetails.setData(JacksonUtils.convertValue(
            accessControlService.filterCaseFieldsByAccess(
                JacksonUtils.convertValueJsonNode(caseDetails.getData()),
                caseType.getCaseFieldDefinitions(),
                accessProfiles,
                CAN_READ,
                false)));
        caseDetails.setDataClassification(JacksonUtils.convertValue(
            accessControlService.filterCaseFieldsByAccess(
                JacksonUtils.convertValueJsonNode(caseDetails.getDataClassification()),
                caseType.getCaseFieldDefinitions(),
                accessProfiles,
                CAN_READ,
                true)));

        return Optional.of(caseDetails);
    }

    /*
     * PART OF FIX.
     * See RestrictedGetCaseOperation for example of auto-wiring AuthorisedGetCaseOperation.
     */
    public Map<String, JsonNode> getFilteredDataClassification(String caseReference,
                                                               CaseTypeDefinition caseType,
                                                               Map<String, JsonNode> fullDataClassification) {
        Set<AccessProfile> accessProfiles = getAccessProfiles(caseReference);
        return JacksonUtils.convertValue(
            accessControlService.filterCaseFieldsByAccess(
                JacksonUtils.convertValueJsonNode(fullDataClassification),
                caseType.getCaseFieldDefinitions(),
                accessProfiles,
                CAN_READ,
                true));
    }
}
