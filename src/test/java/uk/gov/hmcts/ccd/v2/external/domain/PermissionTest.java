package uk.gov.hmcts.ccd.v2.external.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class PermissionTest {

    @Nested
    @DisplayName("getValue()")
    class GetValue {

        @Test
        @DisplayName("should get value of Create Permission")
        void shouldGetValueOfCreate() {
            assertThat(Permission.CREATE.getValue(), is(1));
        }

        @Test
        @DisplayName("should get value of Read Permission")
        void shouldGetValueOfRead() {
            assertThat(Permission.READ.getValue(), is(2));
        }

    }
}
