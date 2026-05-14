package uk.gov.hmcts.ccd.data.caseclosed;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DateCaseClosedRepository extends CrudRepository<DateCaseClosedEntity, Long> {

    List<DateCaseClosedEntity> findByStateChangedDateLessThanEqual(LocalDateTime stateChangedDate);
}
