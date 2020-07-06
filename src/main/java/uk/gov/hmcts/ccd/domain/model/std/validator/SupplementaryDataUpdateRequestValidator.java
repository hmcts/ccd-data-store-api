package uk.gov.hmcts.ccd.domain.model.std.validator;

import java.util.Map;
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

    public void validate(SupplementaryDataUpdateRequest supplementaryData) {
        if (supplementaryData == null
            || supplementaryData.getRequestData() == null
            || supplementaryData.getRequestData().size() == 0) {
            throw new BadRequestException(SUPPLEMENTARY_DATA_INVALID);
        }
        allowedNestedLevels(supplementaryData);
    }

    private void allowedNestedLevels(SupplementaryDataUpdateRequest supplementaryData) {
        supplementaryData
            .getRequestData()
            .entrySet()
            .stream()
            .forEach(entry -> {
                Map<String, Object> operationData = entry.getValue();
                operationData.keySet().stream().forEach(key -> {
                    String[] keys = key.split(Pattern.quote("."));
                    if (keys.length > 2) {
                        throw new BadRequestException(MORE_THAN_ONE_NESTED_LEVEL);
                    }
                });
        });
    }
}
