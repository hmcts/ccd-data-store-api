package uk.gov.hmcts.ccd.data.documentdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DocumentData {

    @JsonProperty("document_url")
    private String url;

    @JsonProperty("document_filename")
    private String filename;

    @JsonProperty("document_binary_url")
    private String binaryUrl;

    @JsonProperty("category_id")
    private String categoryId;
}
