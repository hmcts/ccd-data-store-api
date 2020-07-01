package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import com.vladmihalcea.hibernate.type.json.JsonNodeBinaryType;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.hibernate.query.NativeQuery;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;

public interface SupplementaryDataQueryBuilder {

    List<Query> buildQueryForEachSupplementaryDataProperty(EntityManager entityManager, String caseReference, SupplementaryDataUpdateRequest updateRequest);

    SupplementaryDataOperation operationType();

    default void setCommonProperties(Query query,
                                     String caseReference,
                                     String leafNodeKey,
                                     Object leafNodeValue) {
        query.setParameter("leaf_node_key", "{" + leafNodeKey + "}");
        query.setParameter("value", leafNodeValue);
        query.setParameter("current_time",  LocalDateTime.now(ZoneOffset.UTC));
        query.setParameter("reference", caseReference);
        query.unwrap(NativeQuery.class)
            .addScalar("supplementary_data", JsonNodeBinaryType.INSTANCE);
    }
}
