package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Predicate;

import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.valueOf;

public class SecurityClassificationUtils {
    private static final Logger LOG = LoggerFactory.getLogger(SecurityClassificationUtils.class);

    private static final String ID = "id";
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);

    private SecurityClassificationUtils() {
    }

    public static Predicate<CaseDetails> caseHasClassificationEqualOrLowerThan(SecurityClassification classification) {
        return cd -> Optional.ofNullable(classification).map(sc ->
            sc.higherOrEqualTo(cd.getSecurityClassification())).orElse(false);
    }

    public static Optional<SecurityClassification> getSecurityClassification(JsonNode dataNode) {
        if (dataNode == null || dataNode.isNull()) {
            return Optional.empty();
        }
        if (dataNode.textValue().isEmpty()) {
            return Optional.empty();
        }
        SecurityClassification securityClassification;
        try {
            securityClassification = valueOf(dataNode.textValue());
        } catch (IllegalArgumentException e) {
            LOG.error("Unable to parse security classification for {}", dataNode, e);
            return Optional.empty();
        }
        return Optional.of(securityClassification);
    }


    public static JsonNode getDataClassificationForData(JsonNode data, Iterator<JsonNode> dataIterator) {
        //All the elements of a collection will have the same security classification
        //We can then just return the first element as a representative of the collection's elements security
        // classification
        return dataIterator.hasNext() ? dataIterator.next() : JSON_NODE_FACTORY.nullNode();
    }
}
