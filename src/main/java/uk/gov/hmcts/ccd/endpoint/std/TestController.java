package uk.gov.hmcts.ccd.endpoint.std;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/jcdebugtest")
    public String jcdebugtest() {
        return "jcdebugtest";
    }
}
