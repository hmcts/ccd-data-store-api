package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.TestFixtures;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

class CollectionUtilsTest extends TestFixtures {
    private static final Set<String> SET_A = Set.of("", "  ", "b", "c", "d");
    private static final Set<String> SET_B = Set.of("c", "", "  ", "d", "e", "f");

    @Test
    void testIntersectionResultingSetSizeAndValues() {
        // GIVEN
        final int expectedSize = 4;
        final Set<String> expectedSet = Set.of("c", "d", "", "  ");

        // WHEN
        final Set<String> intersection = CollectionUtils.setsIntersection(SET_A, SET_B);

        // THEN
        assertThat(intersection)
            .isNotNull()
            .hasSize(expectedSize)
            .hasSameElementsAs(expectedSet);
    }

    @ParameterizedTest
    @MethodSource("provideSetsParameters")
    void testShouldRaiseExceptionWhenSetsAreNull(final Set<String> set1, final Set<String> set2) {
        // WHEN
        final Throwable thrown = catchThrowable(() -> CollectionUtils.setsIntersection(set1, set2));

        // THEN
        assertThat(thrown)
            .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @MethodSource("provideMapsParameters")
    void testShouldRaiseExceptionWhenMapsAreNull(final Map<String, String> m1, final Map<String, String> m2) {
        // WHEN
        final Throwable thrown = catchThrowable(() -> CollectionUtils.mapsUnion(m1, m2));

        // THEN
        assertThat(thrown)
            .isInstanceOf(NullPointerException.class);
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> provideSetsParameters() {
        return Stream.of(
            Arguments.of(null, null),
            Arguments.of(SET_A, null),
            Arguments.of(null, SET_B)
        );
    }

}
