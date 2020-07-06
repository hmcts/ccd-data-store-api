package uk.gov.hmcts.ccd.domain.model.std.validator;

import java.util.regex.Pattern;
import javax.inject.Named;
import javax.inject.Singleton;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import static uk.gov.hmcts.ccd.v2.V2.Error.MORE_THAN_ONE_NESTED_LEVEL;
import static uk.gov.hmcts.ccd.v2.V2.Error.SUPPLEMENTARY_DATA_INVALID;

@Named
@Singleton
public class SupplementaryDataUpdateRequestValidator {

    public void validate(SupplementaryDataUpdateRequest supplementaryDataUpdateRequest) {
        if (supplementaryDataUpdateRequest == null
            || !supplementaryDataUpdateRequest.isValidRequestData()) {
            throw new BadRequestException(SUPPLEMENTARY_DATA_INVALID);
        }
        validateAtMostOneLevelOfNesting(supplementaryDataUpdateRequest);
    }

    private void validateAtMostOneLevelOfNesting(SupplementaryDataUpdateRequest supplementaryDataUpdateRequest) {
        for (String name : supplementaryDataUpdateRequest.getPropertiesNames()) {
            String[] keys = name.split(Pattern.quote("."));
            if (keys.length > 2) {
                throw new BadRequestException(MORE_THAN_ONE_NESTED_LEVEL);
            }
        }
    }
}
