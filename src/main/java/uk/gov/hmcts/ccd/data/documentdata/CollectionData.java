package uk.gov.hmcts.ccd.data.documentdata;

import lombok.Data;

import java.util.Map;

@Data
public class CollectionData {

    private String id;

    private Map<String, String> value;
}
