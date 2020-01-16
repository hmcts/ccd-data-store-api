package uk.gov.hmcts.ccd.domain.model.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameterUtil.Parameter.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameterUtil.Parameter.TABLE;
import static uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameterUtil.updateDisplayContextParameter;

class DisplayContextParameterUtilTest {

    @Test
    @DisplayName("should create parameter)")
    void shouldCreateParameter() {
        assertAll(
            () -> assertThat(updateDisplayContextParameter("", COLLECTION, asList("allowInsert", "allowDelete")),
                is("#COLLECTION(allowDelete,allowInsert)")),
            () -> assertThat(updateDisplayContextParameter("", COLLECTION, asList("allowDelete")),
                is("#COLLECTION(allowDelete)")),
            () -> assertThat(updateDisplayContextParameter("", TABLE, asList("postcode")),
                is("#TABLE(postcode)")),
            () -> assertThat(updateDisplayContextParameter("", TABLE, asList("AddressLine1", "postcode")),
                is("#TABLE(AddressLine1,postcode)"))
                 );
    }

    @Test
    @DisplayName("should update parameter)")
    void shouldUpdateParameter() {
        assertAll(
            () -> assertThat(updateDisplayContextParameter("#COLLECTION(allowInsert,allowDelete)", COLLECTION, asList("allowInsert", "allowDelete")),
                is("#COLLECTION(allowDelete,allowInsert)")),
            () -> assertThat(updateDisplayContextParameter("#COLLECTION(allowInsert,allowDelete)", COLLECTION, asList("allowDelete")),
                is("#COLLECTION(allowDelete)")),
            () -> assertThat(updateDisplayContextParameter("#COLLECTION()", COLLECTION, asList("allowInsert")),
                is("#COLLECTION(allowInsert)")),
            () -> assertThat(updateDisplayContextParameter("#TABLE(param1,param2)", TABLE, asList("postcode")),
                is("#TABLE(postcode)"))
                 );
    }

    @Test
    @DisplayName("should update parameter and produce multiple result)")
    void shouldUpdateParameterMultiple() {
        assertAll(
            () -> assertThat(updateDisplayContextParameter("#TABLE()", COLLECTION, asList("allowInsert", "allowDelete")),
                is("#COLLECTION(allowDelete,allowInsert),#TABLE()")),
            () -> assertThat(updateDisplayContextParameter("#TABLE(),#COLLECTION(allowInsert,allowDelete)", COLLECTION, asList("allowDelete")),
                is("#COLLECTION(allowDelete),#TABLE()")),
            () -> assertThat(updateDisplayContextParameter("#TABLE(),#COLLECTION(allowInsert,allowDelete)", TABLE, asList("postcode")),
                is("#COLLECTION(allowDelete,allowInsert),#TABLE(postcode)")),
            () -> assertThat(updateDisplayContextParameter("#TABLE(postcode),#COLLECTION(allowInsert,allowDelete)", COLLECTION, asList("other")),
                is("#COLLECTION(other),#TABLE(postcode)"))
                 );
    }

    @Test
    @DisplayName("should update parameter when display context parameter has spaces)")
    void shouldUpdateParameterWithSpaces() {
        assertAll(
            () -> assertThat(updateDisplayContextParameter("#TABLE( other  ),#COLLECTION(allowInsert ,  allowDelete )", COLLECTION, asList("allowDelete")),
                is("#COLLECTION(allowDelete),#TABLE(other)"))
                 );
    }

    @Test
    @DisplayName("should filter invalid parameters)")
    void shouldFilterInvalidParameters() {
        assertAll(
            () -> assertThat(updateDisplayContextParameter("#INVALID(other),#COLLECTION(allowInsert,allowDelete)",
                COLLECTION, asList("allowDelete")),
                is("#COLLECTION(allowDelete)")),
            () -> assertThat(updateDisplayContextParameter("#COLLECTION(other),#INVALID(allowInsert,allowDelete)",
                COLLECTION, asList("allowDelete")),
                is("#COLLECTION(allowDelete)"))
                 );
    }
}
