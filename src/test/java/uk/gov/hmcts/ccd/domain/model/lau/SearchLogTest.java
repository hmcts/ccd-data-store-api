package uk.gov.hmcts.ccd.domain.model.lau;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static java.time.LocalDateTime.now;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static org.assertj.core.api.Assertions.assertThat;

class SearchLogTest {

    @Test
    void shouldCreateSearchLog() {
        final ZonedDateTime timestamp = ZonedDateTime.of(now(), UTC);
        final SearchLog searchLog = new SearchLog();
        final String caseRefs = "123,567,789";
        final String userId = "789";

        searchLog.setCaseRefs(caseRefs);
        searchLog.setUserId(userId);
        searchLog.setTimestamp(timestamp);

        assertThat(searchLog.getUserId()).isEqualTo(userId);
        assertThat(searchLog.getTimestamp()).isEqualTo(timestamp.format(ISO_INSTANT));
        assertThat(searchLog.getCaseRefs().size()).isEqualTo(3);
        assertThat(searchLog.getCaseRefs().get(0)).isEqualTo("123");
        assertThat(searchLog.getCaseRefs().get(1)).isEqualTo("567");
        assertThat(searchLog.getCaseRefs().get(2)).isEqualTo("789");
    }

    @Test
    void shouldCreateSearchLogViaSearchLogConstructor() {
        final ZonedDateTime timestamp = ZonedDateTime.of(now(), UTC);
        final String caseRefs = "123,567,789";
        final String userId = "789";

        final SearchLog searchLog = new SearchLog();
        searchLog.setUserId(userId);
        searchLog.setCaseRefs(caseRefs);
        searchLog.setTimestamp(timestamp);

        assertThat(searchLog.getUserId()).isEqualTo(userId);
        assertThat(searchLog.getTimestamp()).isEqualTo(timestamp.format(ISO_INSTANT));
        assertThat(searchLog.getCaseRefs().size()).isEqualTo(3);
        assertThat(searchLog.getCaseRefs().get(0)).isEqualTo("123");
        assertThat(searchLog.getCaseRefs().get(1)).isEqualTo("567");
        assertThat(searchLog.getCaseRefs().get(2)).isEqualTo("789");
    }

    @Test
    void shouldNotThrowNpeWhenCaseRefIsNull() {
        final SearchLog searchLog = new SearchLog();
        final String caseRefs = null;
        searchLog.setCaseRefs(caseRefs);

        assertThat(searchLog.getCaseRefs()).isNull();
    }

    @Test
    void shouldTrimCaseRefs() {
        final SearchLog searchLog = new SearchLog();
        final String caseRefs = "   123,   567   ,   789    ";
        searchLog.setCaseRefs(caseRefs);

        assertThat(searchLog.getCaseRefs().size()).isEqualTo(3);
        assertThat(searchLog.getCaseRefs().get(0)).isEqualTo("123");
        assertThat(searchLog.getCaseRefs().get(1)).isEqualTo("567");
        assertThat(searchLog.getCaseRefs().get(2)).isEqualTo("789");
    }
}