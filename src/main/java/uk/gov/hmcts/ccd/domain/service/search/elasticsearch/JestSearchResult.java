package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.searchbox.core.SearchResult;

public class JestSearchResult extends SearchResult {

    public JestSearchResult(SearchResult searchResult) {
        super(searchResult);
    }

    //Compatible with multiple versions （for es5,es6,es7）
    // TODO: remove ASAP under RDM-8901
    //https://github.com/searchbox-io/Jest/issues/656
    @Override
    public Long getTotal() {
        Long total = null;
        JsonElement element = getPath(PATH_TO_TOTAL);
        if (element != null) {
            if (element instanceof JsonPrimitive) {
                return (element).getAsLong();
            } else if (element instanceof JsonObject) {
                total = ((JsonObject) element).getAsJsonPrimitive("value").getAsLong();
            }
        }
        return total;
    }
}
