package uk.gov.hmcts.ccd.data.casedetails;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PRIVATE;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PUBLIC;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.RESTRICTED;

class SecurityClassificationTest {

    @Nested
    @DisplayName("higherOrEqualTo()")
    class HigherOrEqualTo {

        @Test
        @DisplayName("should be false when lower")
        void shouldBeFalseWhenLower() {
            assertThat(PUBLIC.higherOrEqualTo(PRIVATE), is(Boolean.FALSE));
        }

        @Test
        @DisplayName("should be true when equal")
        void shouldBeTrueWhenEqual() {
            assertThat(PRIVATE.higherOrEqualTo(PRIVATE), is(Boolean.TRUE));
        }

        @Test
        @DisplayName("should be true when higher")
        void shouldBeTrueWhenHigher() {
            assertThat(RESTRICTED.higherOrEqualTo(PRIVATE), is(Boolean.TRUE));
        }
    }
}
