package uk.gov.hmcts.ccd.domain.model.casedeletion;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TTLTest {

    public static final String TTL_SUSPENSION_NO_PARAMETERS
        = "uk.gov.hmcts.ccd.domain.model.casedeletion.TTLTest#ttlSuspensionNoParameters";
    public static final String TTL_SUSPENSION_YES_PARAMETERS
        = "uk.gov.hmcts.ccd.domain.model.casedeletion.TTLTest#ttlSuspensionYesParameters";

    @SuppressWarnings("unused")
    public static Stream<Arguments> ttlSuspensionNoParameters() {
        // NB: scope document says: 'N' (or 'No' or 'F' or 'False' or NULL)
        //     however YesNo fields permit only 'Yes', 'No' and null
        return Stream.of(
            Arguments.of(TTL.NO),
            Arguments.of((String)null)
        );
    }

    @SuppressWarnings("unused")
    public static Stream<Arguments> ttlSuspensionYesParameters() {
        // NB: scope document says: 'Y' (or 'Yes' or 'T' or 'True')
        //     however YesNo fields permit only 'Yes', 'No' and null
        return Stream.of(
            Arguments.of(TTL.YES)
        );
    }

    @ParameterizedTest(
        name = "isSuspended No: {0}"
    )
    @MethodSource(TTL_SUSPENSION_NO_PARAMETERS)
    void isSuspended_No(String ttlSuspended) {
        assertFalse(TTL.builder().suspended(ttlSuspended).build().isSuspended());
    }

    @ParameterizedTest(
        name = "isSuspended Yes: {0}"
    )
    @MethodSource(TTL_SUSPENSION_YES_PARAMETERS)
    void isSuspended_Yes(String ttlSuspended) {
        assertTrue(TTL.builder().suspended(ttlSuspended).build().isSuspended());
    }

}
