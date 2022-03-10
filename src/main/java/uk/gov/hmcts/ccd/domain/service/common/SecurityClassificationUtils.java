package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Predicate;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

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

    public static Predicate<CaseTypeDefinition> caseTypeHasClassificationEqualOrLowerThan(
        SecurityClassification classification) {
        return ctd -> Optional.ofNullable(classification).map(sc ->
            sc.higherOrEqualTo(ctd.getSecurityClassification())).orElse(false);
    }

    public static Optional<SecurityClassification> getSecurityClassification(JsonNode dataNode) {
        if (dataNode == null || dataNode.isNull()) {
            return Optional.empty();
        }
        return getSecurityClassification(dataNode.textValue());
    }

    public static Optional<SecurityClassification> getSecurityClassification(String classification) {
        if (StringUtils.isEmpty(classification)) {
            return Optional.empty();
        }
        SecurityClassification securityClassification;
        try {
            securityClassification = valueOf(classification);
        } catch (IllegalArgumentException e) {
            LOG.error("Unable to parse security classification for {}", classification, e);
            return Optional.empty();
        }
        return Optional.of(securityClassification);
    }

    public static JsonNode getDataClassificationForData(JsonNode data, Iterator<JsonNode> dataIterator) {
        Boolean found = false;
        JsonNode dataClassificationValue = null;
        while (dataIterator.hasNext() && !found) {
            dataClassificationValue = dataIterator.next();
            if (null != dataClassificationValue.get(ID)
                && dataClassificationValue.get(ID).equals(data.get(ID))) {
                found = true;
            }
        }
        return found ? dataClassificationValue : JSON_NODE_FACTORY.nullNode();
    }
}
