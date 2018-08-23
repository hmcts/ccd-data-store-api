package uk.gov.hmcts.ccd.domain.model.std.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

@ToString
public class SearchQuery {

    @JsonProperty("native_query")
    public String nativeQuery;

    public String getNativeQuery() {
        return nativeQuery;
    }

    public void setNativeQuery(String nativeQuery) {
        this.nativeQuery = nativeQuery;
    }
}
