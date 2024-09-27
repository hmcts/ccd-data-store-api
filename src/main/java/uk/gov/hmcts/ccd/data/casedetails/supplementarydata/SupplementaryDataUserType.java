package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import com.vladmihalcea.hibernate.type.json.JsonNodeBinaryType;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.SqlTypes;
import com.fasterxml.jackson.databind.JsonNode;

public class SupplementaryDataUserType {

    private static JsonNodeBinaryType type = new JsonNodeBinaryType();
    private BasicTypeReference<JsonNode> ref;

    public SupplementaryDataUserType() {
        ref = new BasicTypeReference<JsonNode>(
            type.getName(), 
            JsonNode.class,
            SqlTypes.OTHER,
            type.getValueConverter()
        );
    }

    public BasicTypeReference<JsonNode> getBasicTypeReference() {
        return ref;
    }

}