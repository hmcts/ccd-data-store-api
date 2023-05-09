package uk.gov.hmcts.ccd.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.searchbox.client.JestClient;
import io.searchbox.core.Bulk;
import io.searchbox.core.Index;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CaseDataLogstashQueueRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDataRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsMapper;

import java.util.Map;
import java.util.stream.Collectors;
import javax.transaction.Transactional;

@Service
@Slf4j
public class ElasticSearchIndexingService {

    @Value("${search.global.index.type}")
    String indexType;

    @Autowired
    CaseDataLogstashQueueRepository queueRepository;

    @Autowired
    CaseDataRepository caseDataRepository;

    @Autowired
    JestClient elasticSearch;

    @Autowired
    CaseDetailsMapper entityMapper;

    @Autowired
    ObjectMapper objectMapper;

    @Scheduled(cron = "${elasticsearch.indexing.frequency}")
    @Transactional
    public void run() {
        var queue = queueRepository.getQueue();
        var caseIds = queue.stream().map(c -> c.getCaseDataId()).collect(Collectors.toList());
        var cases = caseDataRepository.findAllById(caseIds);
        var bulk = new Bulk.Builder().defaultType(indexType);

        for (var caseDetails : cases) {
            try {
                var indexName = caseDetails.getCaseType().toLowerCase() + "_cases";
                var model = entityMapper.entityToModel(caseDetails);
                var json = objectMapper.convertValue(model, Map.class);
                json.put("id", model.getId());
                json.put("reference", model.getReference());
                json.put("data", json.get("case_data"));
                json.remove("case_data");

                bulk.addAction(new Index.Builder(json)
                    .id(caseDetails.getId().toString())
                    .index(indexName)
                    .build());
            } catch (Exception e) {
                log.error("Could not process case: " + caseDetails.getId(), e);
            }
        }

        try {
            elasticSearch.execute(bulk.build());
            queueRepository.deleteAll(queue);
        } catch (Exception e) {
            log.error(e.getMessage());

            throw new RuntimeException("Indexing cases failed");
        }
    }

}
