package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import java.util.Arrays;
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("increment")
public class IncrementSupplementaryDataQueryBuilder implements SupplementaryDataQueryBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(IncrementSupplementaryDataQueryBuilder.class);

    @SuppressWarnings("checkstyle:LineLength") //don't want to break long SQL statement
    private static final String INC_UPDATE_QUERY = "UPDATE case_data SET "
            + "supplementary_data= (CASE"
            + "        WHEN COALESCE(supplementary_data, '{}') = '{}' "
            + "        THEN COALESCE(supplementary_data, '{}') || :json_value\\:\\:jsonb"
            + "        WHEN jsonb_extract_path_text(COALESCE(supplementary_data, '{}'), :node_path) IS NULL "
            + "        THEN jsonb_set(COALESCE(supplementary_data, '{}'), :leaf_node_key, (:value)\\:\\:TEXT\\:\\:jsonb)"
            + "        WHEN jsonb_extract_path_text(COALESCE(supplementary_data, '{}'), :node_path) IS NOT NULL"
            + "        THEN jsonb_set(COALESCE(supplementary_data, '{}'), :leaf_node_key,"
            + "             (jsonb_extract_path_text(supplementary_data, :node_path)\\:\\:INT + :value) \\:\\:TEXT\\:\\:jsonb, false)"
            + "    END) "
            + "WHERE reference = :reference";

    @Override
    public Query build(EntityManager entityManager,
                       String caseReference,
                       String fieldPath,
                       Object fieldValue) {
        Query query = entityManager.createNativeQuery(INC_UPDATE_QUERY);
        setCommonProperties(query, caseReference, fieldPath, fieldValue);
        String jsonValue = requestedDataToJson(fieldPath, fieldValue);
        LOG.info("Before json_value {}, node_path {}, value {}, reference {}",
            jsonValue,
            Arrays.asList(fieldPath.split(Pattern.quote("."))),
            query.getParameter("value"),
            query.getParameter("reference"));
        query.setParameter("value", fieldValue);
        query.setParameter("json_value", jsonValue);
        query.setParameter("node_path", Arrays.asList(fieldPath.split(Pattern.quote("."))));
        LOG.info("After json_value {}, node_path {}, value {}, reference {}",
            query.getParameter("json_value"),
            query.getParameter("node_path"),
            query.getParameter("value"),
            query.getParameter("reference"));
        return query;
    }

    @Override
    public SupplementaryDataOperation operationType() {
        return SupplementaryDataOperation.INC;
    }
}
