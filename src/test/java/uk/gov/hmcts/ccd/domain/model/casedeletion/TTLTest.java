package uk.gov.hmcts.ccd.domain.model.casedeletion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TTLTest {

    // Hide Utility Class Constructor : Utility classes should not have a public or default constructor (squid:S1118)
    private TTLTest() {
    }


    public static final String TTL_SUSPENDED_NO_VALUES
        = "uk.gov.hmcts.ccd.domain.model.casedeletion.TTLTest#ttlSuspendedNoValues";
    public static final String TTL_SUSPENDED_YES_VALUES
        = "uk.gov.hmcts.ccd.domain.model.casedeletion.TTLTest#ttlSuspendedYesValues";

    public static final String TTL_SUSPENDED_VALUES_FOR_IS_SUSPENDED_FALSE
        = "uk.gov.hmcts.ccd.domain.model.casedeletion.TTLTest#ttlSuspendedNoValuesPlusNull";
    public static final String TTL_SUSPENDED_VALUES_FOR_IS_SUSPENDED_TRUE = TTL_SUSPENDED_YES_VALUES;

    private static final LocalDate TODAY = LocalDate.now();
    private static final LocalDate TOMORROW = LocalDate.now().plusDays(1);

    @SuppressWarnings("unused")
    public static Stream<Arguments> ttlSuspendedNoValuesPlusNull() {
        var noArguments = getNoValuesAsArguments();

        // add extra null argument because suspended=null will be treated as suspended=no
        noArguments.add(Arguments.of((String)null));

        return noArguments.stream();
    }

    @SuppressWarnings("unused")
    public static Stream<Arguments> ttlSuspendedNoValues() {
        return getNoValuesAsArguments().stream();
    }

    private static List<Arguments> getNoValuesAsArguments() {
        // NB: scope document says: 'N' (or 'No' or 'F' or 'False' or NULL)
        //     however YesNo fields permit only 'Yes', 'No' and null

        List<Arguments> arguments = new ArrayList<>();
        arguments.add(Arguments.of(TTL.NO));
        arguments.add(Arguments.of(TTL.NO.toUpperCase()));
        arguments.add(Arguments.of(TTL.NO.toLowerCase()));

        return arguments;
    }

    @SuppressWarnings("unused")
    public static Stream<Arguments> ttlSuspendedYesValues() {
        // NB: scope document says: 'Y' (or 'Yes' or 'T' or 'True')
        //     however YesNo fields permit only 'Yes', 'No' and null
        return Stream.of(
            Arguments.of(TTL.YES),
            Arguments.of(TTL.YES.toUpperCase()),
            Arguments.of(TTL.YES.toLowerCase())
        );
    }


    @DisplayName("isSuspended")
    @Nested
    class IsSuspended {

        @ParameterizedTest(
            name = "isSuspended false: {0}"
        )
        @MethodSource(TTL_SUSPENDED_VALUES_FOR_IS_SUSPENDED_FALSE)
        void isSuspended_false(String ttlSuspended) {
            assertFalse(TTL.builder().suspended(ttlSuspended).build().isSuspended());
        }

        @ParameterizedTest(
            name = "isSuspended true: {0}"
        )
        @MethodSource(TTL_SUSPENDED_VALUES_FOR_IS_SUSPENDED_TRUE)
        void isSuspended_true(String ttlSuspended) {
            assertTrue(TTL.builder().suspended(ttlSuspended).build().isSuspended());
        }

    }


    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    @DisplayName("Equals")
    @Nested
    class Equals {

        @ParameterizedTest(
            name = "equals Suspended: 'Yes': {0}"
        )
        @MethodSource(TTL_SUSPENDED_YES_VALUES)
        void equals_suspended_yesValue(String ttlSuspended) {

            // GIVEN
            TTL testA = TTL.builder().suspended(ttlSuspended).build();
            TTL testBMatch = TTL.builder().suspended(TTL.YES).build();
            TTL testBNotMatch = TTL.builder().suspended(TTL.NO).build();
            TTL testNull = TTL.builder().suspended(null).build();

            // WHEN / THEN
            asserTtlEquals_suspendedChecks(true, testA, testA); // i.e. same object
            asserTtlEquals_suspendedChecks(true, testA, testBMatch);
            asserTtlEquals_suspendedChecks(false, testA, testBNotMatch);
            asserTtlEquals_suspendedChecks(false, testA, testNull);
        }

        @ParameterizedTest(
            name = "equals Suspended: 'No': {0}"
        )
        @MethodSource(TTL_SUSPENDED_NO_VALUES)
        void equals_suspended_noValue(String ttlSuspended) {

            // GIVEN
            TTL testA = TTL.builder().suspended(ttlSuspended).build();
            TTL testBMatch = TTL.builder().suspended(TTL.NO).build();
            TTL testBNotMatch = TTL.builder().suspended(TTL.YES).build();
            TTL testNull = TTL.builder().suspended(null).build();

            // WHEN / THEN
            asserTtlEquals_suspendedChecks(true, testA, testA); // i.e. same object
            asserTtlEquals_suspendedChecks(true, testA, testBMatch);
            asserTtlEquals_suspendedChecks(false, testA, testBNotMatch);
            asserTtlEquals_suspendedChecks(false, testA, testNull);
        }

        @DisplayName("equals Suspended: null")
        @Test()
        void equals_suspended_nullValue() {

            // GIVEN;
            TTL testNull = TTL.builder().systemTTL(null).build();
            TTL testOtherNull = TTL.builder().systemTTL(null).build();
            // test matching null against a populated value already covered in equals_suspended_noValue/_yesValue

            // WHEN / THEN
            asserTtlEquals_suspendedChecks(true, testNull, testNull); // i.e. same object
            asserTtlEquals_suspendedChecks(true, testNull, testOtherNull);
        }

        @DisplayName("equals SystemTTL")
        @Test()
        void equals_systemTTL() {

            // GIVEN
            TTL testA = TTL.builder().systemTTL(TODAY).build();
            TTL testBMatch = TTL.builder().systemTTL(TODAY).build();
            TTL testBNotMatch = TTL.builder().systemTTL(TOMORROW).build();
            TTL testNull = TTL.builder().systemTTL(null).build();
            TTL testOtherNull = TTL.builder().systemTTL(null).build();

            // WHEN / THEN
            asserTtlEquals_systemTTLChecks(true, testA, testA); // i.e. same object
            asserTtlEquals_systemTTLChecks(true, testA, testBMatch);
            asserTtlEquals_systemTTLChecks(false, testA, testBNotMatch);
            asserTtlEquals_systemTTLChecks(false, testA, testNull);
            asserTtlEquals_systemTTLChecks(true, testNull, testOtherNull); // null checks;
        }

        @DisplayName("equals OverrideTTL")
        @Test()
        void equals_overrideTTL() {

            // GIVEN
            TTL testA = TTL.builder().overrideTTL(TODAY).build();
            TTL testBMatch = TTL.builder().overrideTTL(TODAY).build();
            TTL testBNotMatch = TTL.builder().overrideTTL(TOMORROW).build();
            TTL testNull = TTL.builder().overrideTTL(null).build();
            TTL testOtherNull = TTL.builder().overrideTTL(null).build();

            // WHEN / THEN
            asserTtlEquals_overrideTTLChecks(true, testA, testA); // i.e. same object
            asserTtlEquals_overrideTTLChecks(true, testA, testBMatch);
            asserTtlEquals_overrideTTLChecks(false, testA, testBNotMatch);
            asserTtlEquals_overrideTTLChecks(false, testA, testNull);
            asserTtlEquals_overrideTTLChecks(true, testNull, testOtherNull); // null checks;
        }

        @SuppressWarnings("ConstantConditions")
        @DisplayName("equals against null object")
        @Test
        void equals_againstNullObject() {

            // WHEN
            var testA = new TTL();
            var resultToNull = testA.equals(null);

            // THEN
            assertFalse(resultToNull);
        }

        @DisplayName("equals against other object")
        @Test
        void equals_againstOtherObject() {

            // WHEN
            var testA = new TTL();
            var resultToOther = testA.equals(new Object());

            // THEN
            assertFalse(resultToOther);
        }

        private void asserTtlEquals_suspendedChecks(boolean expected, TTL testA, TTL testB) {

            asserTtlEquals(expected, testA, testB);

            // additional tests with adjusted data
            TTL ttlAClone = ttlClone(testA);
            TTL ttlBClone = ttlClone(testB);

            // add systemTTL and repeat (i.e. including systemTTL only)
            ttlAClone.setSystemTTL(TODAY);
            ttlBClone.setSystemTTL(TODAY);
            asserTtlEquals(expected, ttlAClone, ttlBClone);
            // add overrideTTL and repeat (i.e. both)
            ttlAClone.setOverrideTTL(TOMORROW);
            ttlBClone.setOverrideTTL(TOMORROW);
            asserTtlEquals(expected, ttlAClone, ttlBClone);
            // remove systemTTL and repeat (i.e. including overrideTTL only)
            ttlAClone.setSystemTTL(null);
            ttlBClone.setSystemTTL(null);
            asserTtlEquals(expected, ttlAClone, ttlBClone);
        }

        private void asserTtlEquals_systemTTLChecks(boolean expected, TTL testA, TTL testB) {

            asserTtlEquals(expected, testA, testB);

            // additional tests with adjusted data
            TTL ttlAClone = ttlClone(testA);
            TTL ttlBClone = ttlClone(testB);

            // add suspended and repeat (i.e. including suspended only)
            ttlAClone.setSuspended(TTL.YES);
            ttlBClone.setSuspended(TTL.YES);
            asserTtlEquals(expected, ttlAClone, ttlBClone);
            // add overrideTTL and repeat (i.e. both)
            ttlAClone.setOverrideTTL(TOMORROW);
            ttlBClone.setOverrideTTL(TOMORROW);
            asserTtlEquals(expected, ttlAClone, ttlBClone);
            // remove suspended and repeat (i.e. including overrideTTL only)
            ttlAClone.setSuspended(null);
            ttlBClone.setSuspended(null);
            asserTtlEquals(expected, ttlAClone, ttlBClone);
        }

        private void asserTtlEquals_overrideTTLChecks(boolean expected, TTL testA, TTL testB) {

            asserTtlEquals(expected, testA, testB);

            // additional tests with adjusted data
            TTL ttlAClone = ttlClone(testA);
            TTL ttlBClone = ttlClone(testB);

            // add suspended and repeat (i.e. including suspended only)
            ttlAClone.setSuspended(TTL.YES);
            ttlBClone.setSuspended(TTL.YES);
            asserTtlEquals(expected, ttlAClone, ttlBClone);
            // add systemTTL and repeat (i.e. both)
            ttlAClone.setSystemTTL(TODAY);
            ttlBClone.setSystemTTL(TODAY);
            asserTtlEquals(expected, ttlAClone, ttlBClone);
            // remove suspended and repeat (i.e. including systemTTL only)
            ttlAClone.setSuspended(null);
            ttlBClone.setSuspended(null);
            asserTtlEquals(expected, ttlAClone, ttlBClone);
        }

        private void asserTtlEquals(boolean expected, TTL testA, TTL testB) {

            // WHEN
            var resultAToB = testA.equals(testB);
            var resultBToA = testB.equals(testA);

            // THEN
            assertAll(
                () -> assertEquals(expected, resultAToB),
                () -> assertEquals(expected, resultBToA)
            );
        }

        private TTL ttlClone(TTL input) {
            return TTL.builder()
                .suspended(input.getSuspended())
                .systemTTL(input.getSystemTTL())
                .overrideTTL(input.getOverrideTTL())
                .build();
        }

    }


    @DisplayName("hashCode")
    @Nested
    class HashCode {

        @ParameterizedTest(
            name = "hashCode Suspended: 'Yes': {0}"
        )
        @MethodSource(TTL_SUSPENDED_YES_VALUES)
        void hashCode_suspended_yesValue(String ttlSuspended) {
            assertHashCode_suspendedChecks(ttlSuspended);
        }

        @ParameterizedTest(
            name = "hashCode Suspended: 'No': {0}"
        )
        @MethodSource(TTL_SUSPENDED_NO_VALUES)
        void hashCode_suspended_noValue(String ttlSuspended) {
            assertHashCode_suspendedChecks(ttlSuspended);
        }

        @DisplayName("hashCode Suspended: null")
        @Test
        void hashCode_suspended_nullValue() {
            assertHashCode_suspendedChecks(null);
        }

        @DisplayName("hashCode SystemTTL")
        @Test
        void hashCode_systemTTL() {

            TTL testTTL = TTL.builder().systemTTL(TODAY).build();

            assertHashCode(testTTL);
            // add suspended and repeat (i.e. including suspended only)
            testTTL.setSuspended(TTL.YES);
            assertHashCode(testTTL);
            // add overrideTTL and repeat (i.e. both)
            testTTL.setOverrideTTL(TOMORROW);
            assertHashCode(testTTL);
            // remove suspended and repeat (i.e. including overrideTTL only)
            testTTL.setSuspended(null);
            assertHashCode(testTTL);
        }

        @DisplayName("hashCode OverrideTTL")
        @Test
        void hashCode_overrideTTLL() {

            TTL testTTL = TTL.builder().overrideTTL(TODAY).build();

            assertHashCode(testTTL);
            // add suspended and repeat (i.e. including suspended only)
            testTTL.setSuspended(TTL.YES);
            assertHashCode(testTTL);
            // add systemTTL and repeat (i.e. both)
            testTTL.setSystemTTL(TODAY);
            assertHashCode(testTTL);
            // remove suspended and repeat (i.e. including systemTTL only)
            testTTL.setSuspended(null);
            assertHashCode(testTTL);
        }

        private void assertHashCode_suspendedChecks(String ttlSuspended) {

            TTL testTTL = TTL.builder().suspended(ttlSuspended).build();

            assertHashCode(testTTL);
            // add systemTTL and repeat (i.e. including systemTTL only)
            testTTL.setSystemTTL(TODAY);
            assertHashCode(testTTL);
            // add overrideTTL and repeat (i.e. both)
            testTTL.setOverrideTTL(TOMORROW);
            assertHashCode(testTTL);
            // remove systemTTL and repeat (i.e. including overrideTTL only)
            testTTL.setSystemTTL(null);
            assertHashCode(testTTL);
        }

        private void assertHashCode(TTL testTTL) {
            assertDoesNotThrow(testTTL::hashCode);
        }
    }

}
