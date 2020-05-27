package uk.gov.hmcts.ccd.datastore.tests.functional.elasticsearch;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.helper.elastic.ElasticsearchTestDataLoaderExtension;

@ExtendWith(ElasticsearchTestDataLoaderExtension.class)
public class ElasticsearchTextAreaFieldTest extends ElasticsearchBaseTest {

    ElasticsearchTextAreaFieldTest(AATHelper aat) {
        super(aat);
    }

    @BeforeAll
    static void setUp() {
        assertElasticsearchEnabled();
    }

    @Nested
    @DisplayName("Tests to verify cases on Text Area Field")
    class DateTimeField {

        @Test
        @DisplayName("should return case for exact match on a text area field")
        void shouldReturnCaseForExactMatchOnDateTimeField() {
          //  searchCaseForExactMatchAndVerifyResponse("TextAreaField", TEXT_AREA);
        }

    }

}



