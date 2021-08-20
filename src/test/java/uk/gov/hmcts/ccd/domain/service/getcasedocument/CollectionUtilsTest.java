package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.TestFixtures;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
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
    @MethodSource("provideSetParameters")
    void testShouldResultInEmptySet(final Set<String> set1, final Set<String> set2) {
        // WHEN
        final Set<String> intersection = CollectionUtils.setsIntersection(set1, set2);

        // THEN
        assertThat(intersection)
            .isNotNull()
            .isEmpty();
    }

    @ParameterizedTest
    @MethodSource("provideNullSetParameters")
    void testShouldRaiseExceptionWhenSetsAreNull(final Set<String> set1, final Set<String> set2) {
        // WHEN
        final Throwable thrown = catchThrowable(() -> CollectionUtils.setsIntersection(set1, set2));

        // THEN
        assertThat(thrown)
            .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @MethodSource("provideNullListParameters")
    void testShouldRaiseExceptionWhenListsAreNull(final List<String> l1, final List<String> l2) {
        // WHEN
        final Throwable thrown = catchThrowable(() -> CollectionUtils.listsUnion(l1, l2));

        // THEN
        assertThat(thrown)
            .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @MethodSource("provideTuple2SetParameters")
    void testShouldUnionLists(final List<String> l1,
                              final List<String> l2,
                              final List<String> expectedList,
                              final int expectedSize) {
        // WHEN
        final List<String> union = CollectionUtils.listsUnion(l1, l2);

        // THEN
        assertThat(union)
            .isNotNull()
            .hasSize(expectedSize)
            .hasSameElementsAs(expectedList);
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> provideNullSetParameters() {
        return Stream.of(
            Arguments.of(null, null),
            Arguments.of(emptySet(), null),
            Arguments.of(null, emptySet()),
            Arguments.of(SET_A, null),
            Arguments.of(null, SET_B)
        );
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> provideSetParameters() {
        return Stream.of(
            Arguments.of(emptySet(), emptySet()),
            Arguments.of(SET_A, emptySet()),
            Arguments.of(emptySet(), SET_B),
            Arguments.of(SET_A, Set.of("x", "y", "z"))
        );
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> provideTuple2SetParameters() {
        final List<Tuple2<String, String>> listA = List.of(new Tuple2<>("a", null), new Tuple2<>("b", null));
        final List<Tuple2<String, String>> listB = List.of(new Tuple2<>("c", "v1"), new Tuple2<>("d", "v2"));
        final List<Tuple2<String, String>> listC = List.of(
            new Tuple2<>("a", null),
            new Tuple2<>("b", null),
            new Tuple2<>("c", "v1"),
            new Tuple2<>("d", "v2")
        );

        return Stream.of(
            Arguments.of(emptyList(), emptyList(), emptyList(), 0),
            Arguments.of(listA, emptyList(), listA, 2),
            Arguments.of(emptyList(), listA, listA, 2),
            Arguments.of(listA, listB, listC, 4)
        );
    }

}
