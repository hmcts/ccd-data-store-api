package uk.gov.hmcts.ccd.v2.external.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
class ThreadProbeController {

    @GetMapping("/thread-info")
    String threadInfo() {
        Thread t = Thread.currentThread();
        log.info("Handling request on {}", t);
        return t.toString();
    }
}
