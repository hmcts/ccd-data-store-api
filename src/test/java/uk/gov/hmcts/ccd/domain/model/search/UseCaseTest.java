package uk.gov.hmcts.ccd.domain.model.search;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class UseCaseTest {

    @Test
    void shouldGetEnumByReference() {
        final UseCase result = UseCase.valueOfReference("ORGCASES");

        assertAll(
            () -> assertThat(result, is(UseCase.ORG_CASES))
        );
    }

    @Test
    void shouldReturnDefaultWhenNoReferenceProvided() {
        final UseCase result = UseCase.valueOfReference(null);

        assertAll(
            () -> assertThat(result, is(UseCase.DEFAULT))
        );
    }

    @Test
    void shouldErrorWhenInvalidReferenceProvided() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> UseCase.valueOfReference("INVALID_REFERENCE"));

        assertAll(
            () -> assertThat(exception.getMessage(), is("Use case reference 'INVALID_REFERENCE' is not a known value."))
        );
    }
}
