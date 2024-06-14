package uk.gov.hmcts.ccd.endpoint.std;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

/*
 * Debugging and logging mechanisms.
 */
@RestController
public class TestController {

    private static final Logger LOG = LoggerFactory.getLogger(TestController.class);

    /*
     * Enable flags (to 1) by default.
     *
     * Curl commands to change flags :-
     * curl -w '\n' https://ccd-data-store-api-pr-2410.preview.platform.hmcts.net/setflag0
     * curl -w '\n' https://ccd-data-store-api-pr-2410.preview.platform.hmcts.net/setflag1
     * curl -w '\n' https://ccd-data-store-api-pr-2410.preview.platform.hmcts.net/setflag2
     * curl -w '\n' https://ccd-data-store-api-pr-2410.preview.platform.hmcts.net/setflag3
     * curl -w '\n' https://ccd-data-store-api-pr-2410.preview.platform.hmcts.net/setflag4
     */
    private static int[] flags = new int[] {1,1,1,1,1};

    /*
     * Method to POST and log messages.
     *
     * Tested originally using curl command below :-
     * curl -X POST https://ccd-data-store-api-pr-2356.preview.platform.hmcts.net/jcdebug -d "TESTING WORKS"
     */
    @PostMapping("/jcdebug")
    public ResponseEntity<String> postMessage(@RequestBody String message) {
        if (message != null) {
            message = message.replaceAll("[\n\r]", "_");
            LOG.debug("JCDEBUG: debug: Message: " + message);
        }
        return ResponseEntity.ok("Message: " + (message == null ? "NULL" : message));
    }

    /*
     * Method to TEST message logging.
     */
    @GetMapping("/jcdebugtest")
    public String jcdebugtest() {
        return jcLog("TEST MESSAGE");
    }

    /*
     * ==== Log message. ====
     */
    public static String jcLog(final String message) {
        String rc = "";
        try {
            final String url = "https://ccd-data-store-api-pr-2410.preview.platform.hmcts.net/jcdebug";
            URL apiUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "text/plain");

            // Write the string payload to the HTTP request body
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(message.getBytes());
            outputStream.flush();
            outputStream.close();
            rc = "Response Code: " + connection.getResponseCode();
        } catch (Exception e) {
            rc = "EXCEPTION";
            e.printStackTrace();
        }
        return "jcLog: " + rc;
    }

    @GetMapping("/setflag0")
    public String setflag0() {
        return setFlag(0);
    }

    @GetMapping("/setflag1")
    public String setflag1() {
        return setFlag(1);
    }

    @GetMapping("/setflag2")
    public String setflag2() {
        return setFlag(2);
    }

    @GetMapping("/setflag3")
    public String setflag3() {
        return setFlag(3);
    }

    @GetMapping("/setflag4")
    public String setflag4() {
        return setFlag(4);
    }

    public static int[] getFlags() {
        return flags;
    }

    /*
     * Set Static Flag.
     */
    private String setFlag(final int index) {
        if (flags[index] == 0) {
            flags[index] = 1;
        } else {
            flags[index] = 0;
        }
        return "flags[" + index + "] = " + flags[index];
    }
}
