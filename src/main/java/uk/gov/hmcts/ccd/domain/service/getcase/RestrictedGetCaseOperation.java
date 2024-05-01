package uk.gov.hmcts.ccd.domain.service.getcase;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
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
import uk.gov.hmcts.ccd.endpoint.exceptions.ForbiddenException;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Service
@Qualifier("restricted")
public class RestrictedGetCaseOperation implements GetCaseOperation {

    private final GetCaseOperation defaultGetCaseOperation;
    private final GetCaseOperation authorisedGetCaseOperation;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseDataAccessControl caseDataAccessControl;
    private final AccessControlService accessControlService;


    @Autowired
    public RestrictedGetCaseOperation(@Qualifier("default") final GetCaseOperation defaultGetCaseOperation,
                                      @Qualifier("authorised") final GetCaseOperation authorisedGetCaseOperation,
                                      @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                          final CaseDefinitionRepository caseDefinitionRepository,
                                      final CaseDataAccessControl caseDataAccessControl,
                                      final AccessControlService accessControlService) {
        this.defaultGetCaseOperation = defaultGetCaseOperation;
        this.authorisedGetCaseOperation = authorisedGetCaseOperation;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseDataAccessControl = caseDataAccessControl;
        this.accessControlService = accessControlService;
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
    public Optional<CaseDetails> execute(String caseReference) {
        Optional<CaseDetails> authorisedCaseDetails = authorisedGetCaseOperation.execute(caseReference);

        if (authorisedCaseDetails.isEmpty()) {
            defaultGetCaseOperation.execute(caseReference)
                .ifPresent(caseDetails -> {
                    CaseTypeDefinition caseTypeDefinition = getCaseType(caseDetails.getCaseTypeId());
                    Set<AccessProfile> accessProfiles = getAccessProfiles(caseDetails);
                    if (hasReadAccess(caseTypeDefinition, accessProfiles, caseDetails)) {
                        throw new ForbiddenException();
                    }
                });
        }

        // size = 287 (and 279)
        jcDebug("RestrictedGetCaseOperation.execute authorisedCaseDetails", authorisedCaseDetails);
        return authorisedCaseDetails;
    }

    private CaseTypeDefinition getCaseType(String caseTypeId) {
        return caseDefinitionRepository.getCaseType(caseTypeId);
    }

    private Set<AccessProfile> getAccessProfiles(CaseDetails caseDetails) {
        return caseDataAccessControl.generateAccessProfilesForRestrictedCase(caseDetails);
    }

    private boolean hasReadAccess(CaseTypeDefinition caseType,
                                  Set<AccessProfile> accessProfiles,
                                  CaseDetails caseDetails) {
        if (caseType == null || caseDetails == null || CollectionUtils.isEmpty(accessProfiles)) {
            return false;
        }

        return accessControlService.canAccessCaseTypeWithCriteria(caseType, accessProfiles, CAN_READ)
            && accessControlService.canAccessCaseStateWithCriteria(caseDetails.getState(), caseType, accessProfiles,
            CAN_READ);
    }
}

