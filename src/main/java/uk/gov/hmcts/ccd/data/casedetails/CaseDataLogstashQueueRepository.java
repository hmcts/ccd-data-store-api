package uk.gov.hmcts.ccd.data.casedetails;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseDataLogstashQueueRepository extends CrudRepository<CaseDetailsLogstashQueueEntity, Long> {

    @Query(value = "SELECT * FROM case_data_logstash_queue FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<CaseDetailsLogstashQueueEntity> getQueue();

}
