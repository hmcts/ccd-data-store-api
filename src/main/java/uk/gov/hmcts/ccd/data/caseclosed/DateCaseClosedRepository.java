package uk.gov.hmcts.ccd.data.caseclosed;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DateCaseClosedRepository extends CrudRepository<DateCaseClosedEntity, Long> {

    List<DateCaseClosedEntity> findByStateChangeDateBefore(LocalDateTime stateChangeDate);

    Optional<DateCaseClosedEntity> findByCcdCaseNumber(Long ccdCaseNumber);

    void deleteByCcdCaseNumber(Long ccdCaseNumber);
}
