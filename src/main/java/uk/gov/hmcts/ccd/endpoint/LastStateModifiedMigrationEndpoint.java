package uk.gov.hmcts.ccd.endpoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.domain.service.laststate.LastStateModifiedMigrationService;

@RestController
public class LastStateModifiedMigrationEndpoint {

    private final LastStateModifiedMigrationService service;

    @Autowired
    public LastStateModifiedMigrationEndpoint(LastStateModifiedMigrationService lastStateModifiedMigrationService) {
        this.service = lastStateModifiedMigrationService;
    }

    @PostMapping(value = "/last-state-modified/migrate")
    public void migrate(@RequestParam(value = "jurisdiction") final String jurisdiction,
                                           @RequestParam(value = "batchSize", required = false, defaultValue = "2000") final int batchSize,
                                           @RequestParam(value = "dryRun", required = false, defaultValue = "true") final boolean dryRun) {
        service.migrate(jurisdiction, batchSize, dryRun);
    }
}

