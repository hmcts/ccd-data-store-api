package uk.gov.hmcts.ccd.domain.service.supplementarydata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class InvalidSupplementaryDataOperationTest {
    public static final LocalDateTime DATE_10_DAYS_AGO = LocalDateTime.now().minusDays(10);
    public static final LocalDateTime DATE_5_DAYS_AHEAD = LocalDateTime.now().plusDays(5);
    public static final String CASE_TYPE = "CASE_TYPE";
    public static final List<String> CASE_TYPES = List.of(CASE_TYPE);

    public static final Integer DEFAULT_LIMIT = 10;

    private InvalidSupplementaryDataOperation instance;

    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    @Mock
    private CaseDetailsToInvalidCaseSupplementaryDataItemMapper mapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        this.instance = new InvalidSupplementaryDataOperation(caseDetailsRepository, mapper);
    }

    @Test
    void shouldDelegateToSupplementaryDataRepository() {

        instance.getInvalidSupplementaryDataCases(CASE_TYPES, DATE_10_DAYS_AGO, Optional.of(DATE_5_DAYS_AHEAD),
            DEFAULT_LIMIT);

        assertAll(
            () -> verify(caseDetailsRepository, times(1))
                .findCasesWithSupplementaryDataHmctsServiceIdButNoOrgsAssignedUsers(CASE_TYPE, DATE_10_DAYS_AGO,
                    Optional.of(DATE_5_DAYS_AHEAD), DEFAULT_LIMIT),
            () -> verify(mapper, times(1)).mapToDataItem(anyList())
        );
    }

    @Test
    void shouldDelegateToSupplementaryDataRepositoryWithDefaultLimitOf10() {

        instance.getInvalidSupplementaryDataCases(CASE_TYPES, DATE_10_DAYS_AGO, Optional.of(DATE_5_DAYS_AHEAD), null);

        assertAll(
            () -> verify(caseDetailsRepository, times(1))
                .findCasesWithSupplementaryDataHmctsServiceIdButNoOrgsAssignedUsers(CASE_TYPE, DATE_10_DAYS_AGO,
                    Optional.of(DATE_5_DAYS_AHEAD), 10),
            () -> verify(mapper, times(1)).mapToDataItem(anyList())
        );
    }
}
