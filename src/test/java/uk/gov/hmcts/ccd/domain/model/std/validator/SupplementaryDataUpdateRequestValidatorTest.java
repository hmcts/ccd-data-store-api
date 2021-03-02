package uk.gov.hmcts.ccd.domain.model.std.validator;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplementaryDataUpdateRequestValidatorTest {

    private SupplementaryDataUpdateRequestValidator requestValidator = new SupplementaryDataUpdateRequestValidator();

    @Test
    @DisplayName("should propagate BadRequestException when supplementary data is null")
    void validate() {
        assertThrows(BadRequestException.class,
            () -> requestValidator.validate(null));
    }

    @Test
    @DisplayName("should propagate BadRequestException when supplementary data not valid")
    void invalidSupplementaryDataUpdateRequest() {
        assertThrows(BadRequestException.class,
            () -> requestValidator.validate(new SupplementaryDataUpdateRequest()));
    }

    @Test
    @DisplayName("should propagate BadRequestException when supplementary data null")
    void shouldThrowBadRequestExceptionWhenSupplementaryDataNull() {
        assertThrows(BadRequestException.class,
            () -> requestValidator.validate(null));
    }

    @Test
    @DisplayName("should propagate BadRequestException when supplementary data has empty operation data")
    void shouldThrowBadRequestExceptionWhenSupplementaryDataHasNoData() {
        assertThrows(BadRequestException.class,
            () -> requestValidator.validate(new SupplementaryDataUpdateRequest(new HashMap<>())));
    }

    @Test
    @DisplayName("should propagate BadRequestException when supplementary data has more than one nested levels")
    void shouldThrowBadRequestExceptionWhenSupplementaryDataHasNestedLevels() {
        SupplementaryDataUpdateRequest request = createRequestDataNested();
        assertThrows(BadRequestException.class,
            () -> requestValidator.validate(request));
    }

    @Test
    @DisplayName("should propagate BadRequestException when supplementary data has more than one nested levels")
    void shouldThrowBadRequestExceptionWhenSupplementaryDataOperationNameNotValid() {
        SupplementaryDataUpdateRequest request = createRequestDataInvalidOperationName();
        BadRequestException badRequestException =  assertThrows(BadRequestException.class,
            () -> requestValidator.validate(request));
        assertTrue(badRequestException.getMessage()
            .endsWith("Unknown supplementary data update operation $ttt"));
    }

    @Test
    @DisplayName("should not propagate BadRequestException when supplementary data has valid data")
    void shouldNotThrowBadRequestExceptionWhenSupplementaryDataValidData() {
        SupplementaryDataUpdateRequest request = createRequestData();
        requestValidator.validate(request);
    }

    private SupplementaryDataUpdateRequest createRequestDataNested() {
        Map<String, Map<String, Object>> requestData = new HashMap<>();
        Map<String, Object> setOperationData = new HashMap<>();
        requestData.put("$set", setOperationData);
        setOperationData.put("orgs_assigned_users.organisationA.organisationB", 32);
        setOperationData.put("orgs_assigned_users.organisationA", 54);

        return new SupplementaryDataUpdateRequest(requestData);
    }

    private SupplementaryDataUpdateRequest createRequestDataInvalidOperationName() {
        Map<String, Map<String, Object>> requestData = new HashMap<>();
        Map<String, Object> setOperationData = new HashMap<>();
        requestData.put("$set", setOperationData);
        setOperationData.put("orgs_assigned_users.organisationA", 32);

        Map<String, Object> invalidOperationName = new HashMap<>();
        requestData.put("$ttt", invalidOperationName);
        invalidOperationName.put("orgs_assigned_users.organisationA", 32);
        return new SupplementaryDataUpdateRequest(requestData);
    }

    private SupplementaryDataUpdateRequest createRequestData() {
        Map<String, Map<String, Object>> requestData = new HashMap<>();
        Map<String, Object> setOperationData = new HashMap<>();
        requestData.put("$set", setOperationData);
        setOperationData.put("orgs_assigned_users.organisationC", 32);
        setOperationData.put("orgs_assigned_users.organisationA", 54);

        return new SupplementaryDataUpdateRequest(requestData);
    }
}
