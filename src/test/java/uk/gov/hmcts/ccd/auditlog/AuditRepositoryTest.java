package uk.gov.hmcts.ccd.auditlog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;

public class AuditRepositoryTest {

    private AuditRepository repository;

    @Mock
    private AuditLogFormatter logFormatter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        repository = new LoggerAuditRepository(logFormatter);
    }

    @Test
    public void shouldSaveAuditEntry() {
        AuditEntry auditEntry = new AuditEntry();

        repository.save(auditEntry);

        verify(logFormatter).format(auditEntry);
    }
}
