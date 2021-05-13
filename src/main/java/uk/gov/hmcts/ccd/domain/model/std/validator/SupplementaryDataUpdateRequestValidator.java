package uk.gov.hmcts.ccd.domain.model.std.validator;

import java.util.Optional;
import java.util.regex.Pattern;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.data.casedetails.supplementarydata.SupplementaryDataOperation;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import static uk.gov.hmcts.ccd.v2.V2.Error.MORE_THAN_ONE_NESTED_LEVEL;
import static uk.gov.hmcts.ccd.v2.V2.Error.SUPPLEMENTARY_DATA_UPDATE_INVALID;
import static uk.gov.hmcts.ccd.v2.V2.Error.UNKNOWN_SUPPLEMENTARY_UPDATE_OPERATION;

@Named
@Singleton
public class SupplementaryDataUpdateRequestValidator {

    private static final Logger LOG = LoggerFactory.getLogger(SupplementaryDataUpdateRequestValidator.class);

    public void validate(SupplementaryDataUpdateRequest supplementaryDataUpdateRequest) {
        LOG.error("Supplementary data validation started");
        if (supplementaryDataUpdateRequest == null
            || !supplementaryDataUpdateRequest.isValidRequestData()) {
            throw new BadRequestException(SUPPLEMENTARY_DATA_UPDATE_INVALID);
        }
        validateAtMostOneLevelOfNesting(supplementaryDataUpdateRequest);
        validateRequestOperations(supplementaryDataUpdateRequest);
        LOG.error("Supplementary data validation completed");
    }

    private void validateAtMostOneLevelOfNesting(SupplementaryDataUpdateRequest supplementaryDataUpdateRequest) {
        LOG.error("validateAtMostOneLevelOfNesting started");
        for (String name : supplementaryDataUpdateRequest.getPropertiesNames()) {
            String[] keys = name.split(Pattern.quote("."));
            if (keys.length > 2) {
                throw new BadRequestException(MORE_THAN_ONE_NESTED_LEVEL);
            }
        }
        LOG.error("validateAtMostOneLevelOfNesting completed");
    }

    private void validateRequestOperations(SupplementaryDataUpdateRequest supplementaryDataUpdateRequest) {
        LOG.error("validateRequestOperations started");
        for (String operationName : supplementaryDataUpdateRequest.getOperations()) {
            Optional<SupplementaryDataOperation> operation = SupplementaryDataOperation.getOperation(operationName);
            if (!operation.isPresent()) {
                throw new BadRequestException(UNKNOWN_SUPPLEMENTARY_UPDATE_OPERATION + " " + operationName);
            }
        }
        LOG.error("validateRequestOperations completed");
    }
}
