package uk.gov.hmcts.ccd.domain.service.getcase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import java.util.Map;
import java.util.Optional;

import static uk.gov.hmcts.ccd.endpoint.std.TestController.jcLog;

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

    @Autowired
    private ObjectMapper objectMapper;

    private void jcLogJsonNodeValue(final String message, final JsonNode value) {
        try {
            jcLog(message + " " + value.size() + " " + value.hashCode() + " "
                + objectMapper.writeValueAsString(value).hashCode());
        } catch (Exception e) {
            jcLog(message + " EXCEPTION: " + e.getMessage());
        }
    }

    @Override
    public Optional<CaseDetails> execute(final String jurisdictionId,
                                         final String caseTypeId,
                                         final String caseReference) {
        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference is not valid");
        }

        return Optional.ofNullable(caseDetailsRepository.findUniqueCase(jurisdictionId, caseTypeId, caseReference));
    }

    /*
     * QUESTION :-
     * In scenario where hashCodes  defaultDataClassification_Value  !=  callbackDataClassification_Value
     * Are the hashCodes  defaultDataClassification_Value  ==  dataClassification_Value
     */
    @Override
    public Optional<CaseDetails> execute(String caseReference) {
        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference is not valid");
        }

        // BELOW: JC debugging
        final CaseDetails caseDetails = caseDetailsRepository.findByReference(Long.valueOf(caseReference));
        try {
            final Map<String, JsonNode> dataClassification = caseDetails.getDataClassification();
            final JsonNode dataClassification_Value = JacksonUtils.convertValueJsonNode(dataClassification);
            jcLogJsonNodeValue("JCDEBUG2: DefaultGetCaseOperation.execute(): "
                + "dataClassification_Value", dataClassification_Value);
        } catch (Exception e) {
            jcLog("JCDEBUG2: DefaultGetCaseOperation.execute(): EXCEPTION: " + e.getMessage());
        }
        // BELOW: JC debugging

        return Optional.ofNullable(caseDetails);
    }
}
