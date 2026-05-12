package uk.gov.hmcts.ccd.data.caseclosed;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "date_case_closed")
public class DateCaseClosedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "date_case_closed_id_seq_generator")
    @SequenceGenerator(
        name = "date_case_closed_id_seq_generator",
        sequenceName = "date_case_closed_id_seq",
        allocationSize = 1
    )
    private Long id;

    @Column(name = "ccd_case_number", nullable = false)
    private Long ccdCaseNumber;

    @Column(name = "state")
    private String state;

    @Column(name = "state_category")
    private String stateCategory;

    @Column(name = "state_changed_date")
    private LocalDateTime stateChangedDate;
}
