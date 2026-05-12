package uk.gov.hmcts.ccd.data.caseclosed;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Transactional
@Repository
public interface DateCaseClosedRepository extends CrudRepository<DateCaseClosedEntity, Long> {

    List<DateCaseClosedEntity> findClosedCasesByStateChangedDate(LocalDateTime stateChangedDate);
}
