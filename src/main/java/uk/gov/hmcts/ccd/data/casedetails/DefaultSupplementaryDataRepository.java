package uk.gov.hmcts.ccd.data.casedetails;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonNodeBinaryType;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;
import org.hibernate.query.NativeQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;

@Named
@Qualifier("default")
@Singleton
@Transactional
public class DefaultSupplementaryDataRepository implements SupplementaryDataRepository {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultSupplementaryDataRepository.class);

    @PersistenceContext
    private EntityManager em;

    private CaseDetailsRepository caseDetailsRepository;

    private String SET_UPDATE_QUERY = "UPDATE case_data "
        + "SET supplementary_data = jsonb_set(supplementary_data, :path, to_jsonb(:param_value), true), "
        + "supplementary_data_last_modified = :current_time "
        + "WHERE reference = :reference";

    private String INC_UPDATE_QUERY = "UPDATE case_data  "
        + "SET supplementary_data = jsonb_set(supplementary_data, :path, "
        + "to_jsonb(CAST(CAST(COALESCE(supplementary_data->:element_path, '0') AS VARCHAR) AS INTEGER) + CAST(:param_value AS INTEGER))), "
        + "supplementary_data_last_modified = :current_time "
        + "WHERE reference = :reference";

    private String SUPPLEMENTARY_DATA_QUERY = "SELECT cd.supplementary_data FROM case_data cd WHERE cd.reference = :reference";



    @Override
    public void setSupplementaryData(String caseReference, Map<String, Object> supplementaryData) {
        LOG.debug("Set supplementary data");
        supplementaryData.keySet().forEach(key -> {
            Query query = executeQuery(SET_UPDATE_QUERY, key, supplementaryData.get(key), caseReference, LocalDateTime.now(ZoneOffset.UTC));
            query.executeUpdate();
        });
    }

    @Override
    public void incrementSupplementaryData(String caseReference, Map<String, Object> supplementaryData) {
        LOG.debug("Insert supplementary data");
        supplementaryData.keySet().forEach(key -> {
            Query query = executeQuery(INC_UPDATE_QUERY, key, supplementaryData.get(key), caseReference, LocalDateTime.now(ZoneOffset.UTC));
            query.setParameter("element_path", createElementPath(key));
            query.executeUpdate();
        });
    }

    @Override
    public SupplementaryData findSupplementaryData(String caseReference) {
        LOG.debug("Find supplementary data");
        Query selectQuery = em.createNativeQuery(SUPPLEMENTARY_DATA_QUERY);
        selectQuery.setParameter("reference", caseReference);
        selectQuery.unwrap(org.hibernate.query.NativeQuery.class)
            .addScalar("supplementary_data", JsonNodeBinaryType.INSTANCE);
        JsonNode result = (JsonNode) selectQuery.getSingleResult();
        return new SupplementaryData(JacksonUtils.convertJsonNode(result));
    }

    private Query executeQuery(String queryName, String path, Object value, String reference, LocalDateTime localDateTime) {
        Query query = em.createNativeQuery(queryName);
        query.setParameter("reference", reference);
        query.setParameter("path", "{" + path + "}");
        query.setParameter("current_time", localDateTime);
        query.setParameter("param_value", value);
        query.unwrap(NativeQuery.class)
            .addScalar("supplementary_data", JsonNodeBinaryType.INSTANCE);
        return query;
    }

    private String createElementPath (String key) {
        StringBuffer sb = new StringBuffer(key);
        int index = key.lastIndexOf(",");
        sb.replace(index, index + 1, "->>");
        return sb.toString().replaceAll(",", "->");
    }
}
