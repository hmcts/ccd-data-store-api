package uk.gov.hmcts.ccd.domain.model.search.elasticsearch;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class SearchResultViewItem {

    @JsonProperty("case_id")
    private String caseId;
    private Map<String, Object> fields;
    @JsonProperty("fields_formatted")
    private Map<String, Object> fieldsFormatted;
}
