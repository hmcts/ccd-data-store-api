package uk.gov.hmcts.ccd.data.casedetails.search;

import org.junit.Test;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ParameterSanitiserTest {

    FieldMapSanitizeOperation subject = new FieldMapSanitizeOperation();

    public ParameterSanitiserTest() {

    }

    @Test(expected = BadRequestException.class)
    public void checkSpaceinFieldName() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("case.field.with a space", "anything");
        subject.execute(params);
    }

    @Test
    public void checkSpaceAtEndOfFieldName() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("case.field", "anything   ");
        params = subject.execute(params);
        assertEquals(params.get("field"), "anything");
    }
}
