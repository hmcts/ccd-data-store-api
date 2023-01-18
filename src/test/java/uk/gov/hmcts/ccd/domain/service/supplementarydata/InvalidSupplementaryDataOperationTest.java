package uk.gov.hmcts.ccd.domain.service.supplementarydata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.supplementarydata.SupplementaryDataRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class InvalidSupplementaryDataOperationTest {
    public static final LocalDateTime DATE_10_DAYS_AGO = LocalDateTime.now().minusDays(10);
    public static final LocalDateTime DATE_5_DAYS_AHEAD = LocalDateTime.now().plusDays(5);
    public static final Integer DEFAULT_LIMIT = 10;

    private InvalidSupplementaryDataOperation instance;

    @Mock
    private SupplementaryDataRepository supplementaryDataRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        this.instance = new InvalidSupplementaryDataOperation(supplementaryDataRepository);
    }

    @Test
    void shouldDelegateToSupplementaryDataRepository() {

        instance.getInvalidSupplementaryDataCases(DATE_10_DAYS_AGO, Optional.of(DATE_5_DAYS_AHEAD), DEFAULT_LIMIT);

        assertAll(
            () -> verify(supplementaryDataRepository, times(1))
                .findCasesWithSupplementaryDataHmctsServiceIdButNoOrgsAssignedUsers(DATE_10_DAYS_AGO,
                    Optional.of(DATE_5_DAYS_AHEAD), DEFAULT_LIMIT)
        );
    }

    @Test
    void shouldDelegateToSupplementaryDataRepositoryWithDefaultLimitOf10() {

        instance.getInvalidSupplementaryDataCases(DATE_10_DAYS_AGO, Optional.of(DATE_5_DAYS_AHEAD), null);

        assertAll(
            () -> verify(supplementaryDataRepository, times(1))
                .findCasesWithSupplementaryDataHmctsServiceIdButNoOrgsAssignedUsers(DATE_10_DAYS_AGO,
                    Optional.of(DATE_5_DAYS_AHEAD), 10)
        );
    }
}
