package uk.gov.hmcts.ccd.domain.service.caseclosed;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.caseclosed.DateCaseClosedEntity;
import uk.gov.hmcts.ccd.data.caseclosed.DateCaseClosedRepository;
import uk.gov.hmcts.ccd.domain.model.search.DateCaseClosedResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Qualifier(DefaultClosedCaseSearchOperation.QUALIFIER)
public class DefaultClosedCaseSearchOperation implements ClosedCaseSearchOperation {

    public static final String QUALIFIER = "default";
    private final DateCaseClosedRepository dateCaseClosedRepository;

    public DefaultClosedCaseSearchOperation(DateCaseClosedRepository dateCaseClosedRepository) {
        this.dateCaseClosedRepository = dateCaseClosedRepository;
    }

    @Override
    public DateCaseClosedResponse execute(LocalDate closedCaseDate) {
        LocalDateTime nextDayStart = closedCaseDate.plusDays(1).atStartOfDay();
        List<String> caseReferences = dateCaseClosedRepository.findByStateChangedDateBefore(nextDayStart)
            .stream()
            .map(DateCaseClosedEntity::getCcdCaseNumber)
            .map(String::valueOf)
            .distinct()
            .toList();

        return new DateCaseClosedResponse(caseReferences);
    }
}
