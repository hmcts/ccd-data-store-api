package uk.gov.hmcts.ccd.domain.service.caseclosed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.data.caseclosed.DateCaseClosedEntity;
import uk.gov.hmcts.ccd.data.caseclosed.DateCaseClosedRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DateCaseClosedServiceTest {

    private static final Long CASE_REFERENCE = 1234567890123456L;
    private static final String CLOSED_STATE = "ClosedForPayment";
    private static final String OPEN_STATE = "Open";
    private static final LocalDateTime STATE_CHANGED_DATE = LocalDateTime.of(2026, 6, 16, 10, 30);

    @Mock
    private DateCaseClosedRepository dateCaseClosedRepository;

    @Mock
    private CaseTypeService caseTypeService;

    private CaseTypeDefinition caseTypeDefinition;
    private DateCaseClosedService dateCaseClosedService;

    @BeforeEach
    void setUp() {
        caseTypeDefinition = new CaseTypeDefinition();
        dateCaseClosedService = new DateCaseClosedService(dateCaseClosedRepository, caseTypeService);
    }

    @Test
    void shouldSaveDateCaseClosedWhenNewCaseStateCategoryContainsClosedForPayment() {
        CaseDetails caseDetails = caseDetails(CLOSED_STATE);
        when(caseTypeService.findState(caseTypeDefinition, CLOSED_STATE))
            .thenReturn(state("CLOSED FOR PAYMENT, End"));
        when(dateCaseClosedRepository.findByCcdCaseNumber(CASE_REFERENCE)).thenReturn(Optional.empty());

        dateCaseClosedService.updateForNewCase(caseDetails, caseTypeDefinition);

        ArgumentCaptor<DateCaseClosedEntity> captor = ArgumentCaptor.forClass(DateCaseClosedEntity.class);
        verify(dateCaseClosedRepository).save(captor.capture());

        DateCaseClosedEntity dateCaseClosedEntity = captor.getValue();
        assertAll(
            () -> assertThat(dateCaseClosedEntity.getCcdCaseNumber()).isEqualTo(CASE_REFERENCE),
            () -> assertThat(dateCaseClosedEntity.getState()).isEqualTo(CLOSED_STATE),
            () -> assertThat(dateCaseClosedEntity.getStateCategory()).isEqualTo("CLOSED FOR PAYMENT, End"),
            () -> assertThat(dateCaseClosedEntity.getStateChangedDate()).isEqualTo(STATE_CHANGED_DATE)
        );
    }

    @Test
    void shouldNotSaveDateCaseClosedWhenNewCaseStateCategoryDoesNotContainClosedForPayment() {
        CaseDetails caseDetails = caseDetails(OPEN_STATE);
        when(caseTypeService.findState(caseTypeDefinition, OPEN_STATE)).thenReturn(state("End"));

        dateCaseClosedService.updateForNewCase(caseDetails, caseTypeDefinition);

        verify(dateCaseClosedRepository, never()).save(any(DateCaseClosedEntity.class));
    }

    @Test
    void shouldNotSaveDateCaseClosedWhenStateCategoryOnlyPartiallyMatchesClosedForPayment() {
        CaseDetails caseDetails = caseDetails(OPEN_STATE);
        when(caseTypeService.findState(caseTypeDefinition, OPEN_STATE))
            .thenReturn(state("CLOSED FOR PAYMENT EXAMPLE, End"));

        dateCaseClosedService.updateForNewCase(caseDetails, caseTypeDefinition);

        verify(dateCaseClosedRepository, never()).save(any(DateCaseClosedEntity.class));
    }

    @Test
    void shouldSaveDateCaseClosedWhenCaseEventCurrentStateCategoryContainsClosedForPayment() {
        CaseDetails caseDetails = caseDetails(CLOSED_STATE);
        final CaseDetails caseDetailsBefore = caseDetails(OPEN_STATE);
        when(caseTypeService.findState(caseTypeDefinition, CLOSED_STATE))
            .thenReturn(state("CLOSED FOR PAYMENT, End"));
        when(caseTypeService.findState(caseTypeDefinition, OPEN_STATE)).thenReturn(state("End"));
        when(dateCaseClosedRepository.findByCcdCaseNumber(CASE_REFERENCE)).thenReturn(Optional.empty());

        dateCaseClosedService.updateForCaseEvent(caseDetails, caseDetailsBefore, caseTypeDefinition);

        verify(dateCaseClosedRepository).save(any(DateCaseClosedEntity.class));
        verify(dateCaseClosedRepository, never()).deleteByCcdCaseNumber(any());
    }

    @Test
    void shouldUpdateExistingDateCaseClosedWhenPreviousStateCategoryAlsoContainsClosedForPayment() {
        CaseDetails caseDetails = caseDetails(CLOSED_STATE);
        final CaseDetails caseDetailsBefore = caseDetails("PreviousClosedForPayment");
        DateCaseClosedEntity existingDateCaseClosedEntity = new DateCaseClosedEntity();
        existingDateCaseClosedEntity.setId(1L);
        existingDateCaseClosedEntity.setCcdCaseNumber(CASE_REFERENCE);
        existingDateCaseClosedEntity.setState("PreviousClosedForPayment");
        existingDateCaseClosedEntity.setStateCategory("CLOSED FOR PAYMENT, Archived");
        when(caseTypeService.findState(caseTypeDefinition, CLOSED_STATE))
            .thenReturn(state("CLOSED FOR PAYMENT, End"));
        when(caseTypeService.findState(caseTypeDefinition, "PreviousClosedForPayment"))
            .thenReturn(state("CLOSED FOR PAYMENT, Archived"));
        when(dateCaseClosedRepository.findByCcdCaseNumber(CASE_REFERENCE))
            .thenReturn(Optional.of(existingDateCaseClosedEntity));

        dateCaseClosedService.updateForCaseEvent(caseDetails, caseDetailsBefore, caseTypeDefinition);

        ArgumentCaptor<DateCaseClosedEntity> captor = ArgumentCaptor.forClass(DateCaseClosedEntity.class);
        verify(dateCaseClosedRepository).save(captor.capture());
        verify(dateCaseClosedRepository, never()).deleteByCcdCaseNumber(any());
        assertAll(
            () -> assertThat(captor.getValue().getId()).isEqualTo(1L),
            () -> assertThat(captor.getValue().getCcdCaseNumber()).isEqualTo(CASE_REFERENCE),
            () -> assertThat(captor.getValue().getState()).isEqualTo(CLOSED_STATE),
            () -> assertThat(captor.getValue().getStateCategory()).isEqualTo("CLOSED FOR PAYMENT, End"),
            () -> assertThat(captor.getValue().getStateChangedDate()).isEqualTo(STATE_CHANGED_DATE)
        );
    }

    @Test
    void shouldDeleteDateCaseClosedWhenCaseEventMovesOutOfClosedForPaymentStateCategory() {
        CaseDetails caseDetails = caseDetails(OPEN_STATE);
        CaseDetails caseDetailsBefore = caseDetails(CLOSED_STATE);
        when(caseTypeService.findState(caseTypeDefinition, OPEN_STATE)).thenReturn(state("End"));
        when(caseTypeService.findState(caseTypeDefinition, CLOSED_STATE))
            .thenReturn(state("CLOSED FOR PAYMENT, End"));

        dateCaseClosedService.updateForCaseEvent(caseDetails, caseDetailsBefore, caseTypeDefinition);

        verify(dateCaseClosedRepository).deleteByCcdCaseNumber(CASE_REFERENCE);
        verify(dateCaseClosedRepository, never()).save(any(DateCaseClosedEntity.class));
    }

    @Test
    void shouldLeaveOneDateCaseClosedRowWhenCaseLeavesAndReEntersClosedForPayment() {
        final CaseDetails closedCaseDetails = caseDetails(CLOSED_STATE);
        final CaseDetails openCaseDetails = caseDetails(OPEN_STATE);
        when(caseTypeService.findState(caseTypeDefinition, CLOSED_STATE))
            .thenReturn(state("CLOSED FOR PAYMENT, End"));
        when(caseTypeService.findState(caseTypeDefinition, OPEN_STATE)).thenReturn(state("End"));
        when(dateCaseClosedRepository.findByCcdCaseNumber(CASE_REFERENCE)).thenReturn(Optional.empty());

        dateCaseClosedService.updateForCaseEvent(openCaseDetails, closedCaseDetails, caseTypeDefinition);
        dateCaseClosedService.updateForCaseEvent(closedCaseDetails, openCaseDetails, caseTypeDefinition);

        ArgumentCaptor<DateCaseClosedEntity> captor = ArgumentCaptor.forClass(DateCaseClosedEntity.class);
        verify(dateCaseClosedRepository).deleteByCcdCaseNumber(CASE_REFERENCE);
        verify(dateCaseClosedRepository).findByCcdCaseNumber(CASE_REFERENCE);
        verify(dateCaseClosedRepository).save(captor.capture());
        assertAll(
            () -> assertThat(captor.getValue().getCcdCaseNumber()).isEqualTo(CASE_REFERENCE),
            () -> assertThat(captor.getValue().getState()).isEqualTo(CLOSED_STATE),
            () -> assertThat(captor.getValue().getStateCategory()).isEqualTo("CLOSED FOR PAYMENT, End"),
            () -> assertThat(captor.getValue().getStateChangedDate()).isEqualTo(STATE_CHANGED_DATE)
        );
    }

    private CaseDetails caseDetails(String state) {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setReference(CASE_REFERENCE);
        caseDetails.setState(state);
        caseDetails.setLastStateModifiedDate(STATE_CHANGED_DATE);
        return caseDetails;
    }

    private CaseStateDefinition state(String stateCategory) {
        CaseStateDefinition state = new CaseStateDefinition();
        state.setStateCategory(stateCategory);
        return state;
    }
}
