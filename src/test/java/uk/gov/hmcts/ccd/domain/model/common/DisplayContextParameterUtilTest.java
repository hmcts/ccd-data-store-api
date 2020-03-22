package uk.gov.hmcts.ccd.domain.model.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameterUtil.updateCollectionDisplayContextParameter;

class DisplayContextParameterUtilTest {

    @Test
    @DisplayName("should create collection parameter)")
    void shouldCreateCollectionParameter() {
        assertAll(
            () -> assertThat(updateCollectionDisplayContextParameter("", asList("allowInsert", "allowDelete")),
                is("#COLLECTION(allowDelete,allowInsert)")),
            () -> assertThat(updateCollectionDisplayContextParameter("", asList("allowInsert")),
                is("#COLLECTION(allowInsert)")),
            () -> assertThat(updateCollectionDisplayContextParameter("", asList("allowDelete")),
                is("#COLLECTION(allowDelete)")),
            () -> assertThat(updateCollectionDisplayContextParameter("", asList("")),
                is("#COLLECTION()"))
                 );
    }

    @Test
    @DisplayName("should update parameter)")
    void shouldUpdateParameter() {
        assertAll(
            () -> assertThat(updateCollectionDisplayContextParameter("#COLLECTION(allowInsert,allowDelete)", asList("allowInsert", "allowDelete")),
                is("#COLLECTION(allowDelete,allowInsert)")),
            () -> assertThat(updateCollectionDisplayContextParameter("#COLLECTION(allowInsert)", asList("allowInsert", "allowDelete")),
                is("#COLLECTION(allowDelete,allowInsert)")),
            () -> assertThat(updateCollectionDisplayContextParameter("#COLLECTION(allowInsert,allowDelete)", asList("allowDelete", "allowInsert")),
                is("#COLLECTION(allowDelete,allowInsert)")),
            () -> assertThat(updateCollectionDisplayContextParameter("#COLLECTION(allowInsert,allowDelete)", asList("allowDelete")),
                is("#COLLECTION(allowDelete)")),
            () -> assertThat(updateCollectionDisplayContextParameter("#COLLECTION(other,allowInsert,allowDelete, other2)", asList("allowDelete")),
                is("#COLLECTION(allowDelete,other,other2)")),
            () -> assertThat(updateCollectionDisplayContextParameter("#COLLECTION()", asList("allowInsert")),
                is("#COLLECTION(allowInsert)"))
                 );
    }

    @Test
    @DisplayName("should update parameter and produce multiple result)")
    void shouldUpdateParameterMultiple() {
        assertAll(
            () -> assertThat(updateCollectionDisplayContextParameter("#TABLE()", asList("allowInsert", "allowDelete")),
                is("#TABLE(),#COLLECTION(allowDelete,allowInsert)")),
            () -> assertThat(updateCollectionDisplayContextParameter("#TABLE(),#COLLECTION(allowInsert,allowDelete)", asList("allowDelete")),
                is("#TABLE(),#COLLECTION(allowDelete)")),
            () -> assertThat(updateCollectionDisplayContextParameter("#TABLE(),#COLLECTION(other,allowInsert,allowDelete)", asList("allowDelete")),
                is("#TABLE(),#COLLECTION(allowDelete,other)")),
            () -> assertThat(updateCollectionDisplayContextParameter("#TABLE(postcode),#COLLECTION(other,allowInsert,allowDelete)", asList("allowDelete")),
                is("#TABLE(postcode),#COLLECTION(allowDelete,other)")),
            () -> assertThat(updateCollectionDisplayContextParameter("#TABLE(postcode),#COLLECTION(other, allowInsert,allowDelete)", asList("allowInsert", "allowDelete")),
                is("#TABLE(postcode),#COLLECTION(allowDelete,allowInsert,other)"))
                 );
    }

    @Test
    @DisplayName("should update parameter when display context parameter has spaces)")
    void shouldUpdateParameterWithSpaces() {
        assertAll(
            () -> assertThat(updateCollectionDisplayContextParameter("#TABLE( other  ),#COLLECTION(allowInsert ,  allowDelete )", asList("allowDelete")),
                is("#TABLE( other  ),#COLLECTION(allowDelete)"))
                 );
    }

    @Test
    @DisplayName("should remove duplicates from parameter values)")
    void shouldRemoveDuplicatesFromParameterValues() {
        assertAll(
            () -> assertThat(updateCollectionDisplayContextParameter("#COLLECTION(allowInsert,allowDelete)", asList("allowDelete" , "allowInsert", "allowInsert")),
                is("#COLLECTION(allowDelete,allowInsert)"))
                 );
    }

    @Test
    @DisplayName("should update parameter when display context parameter is null)")
    void shouldUpdateParameterWhenIsNull() {
        assertAll(
            () -> assertThat(updateCollectionDisplayContextParameter(null, asList("allowInsert", "allowDelete")),
                is("#COLLECTION(allowDelete,allowInsert)")),
            () -> assertThat(updateCollectionDisplayContextParameter(null, asList("allowDelete")),
                is("#COLLECTION(allowDelete)")),
            () -> assertThat(updateCollectionDisplayContextParameter(null, asList("")),
                is("#COLLECTION()"))
                 );
    }
}
