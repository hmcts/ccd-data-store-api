package uk.gov.hmcts.ccd.domain.model.draft;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DraftList {

    private List<Draft> data;

    @JsonProperty("paging_cursors")
    private PagingCursors paging;

    public List<Draft> getData() {
        return data;
    }

    public void setData(List<Draft> data) {
        this.data = data;
    }

    public PagingCursors getPaging() {
        return paging;
    }

    public void setPaging(PagingCursors paging) {
        this.paging = paging;
    }

    static class PagingCursors {
        private String after;

        public String getAfter() {
            return after;
        }

        public void setAfter(String after) {
            this.after = after;
        }
    }
}
