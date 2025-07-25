package uk.gov.hmcts.ccd.data.casedetails.search;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ParameterSanitiserTest {

    FieldMapSanitizeOperation subject = new FieldMapSanitizeOperation();

    public ParameterSanitiserTest() {

    }

    @Test
    public void checkSpaceinFieldName() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("case.field.with a space", "anything");
        assertThrows(BadRequestException.class, () -> subject.execute(params));
    }

    @Test
    public void checkSpaceAtEndOfFieldName() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("case.field", "anything   ");
        params = subject.execute(params);
        assertEquals(params.get("field"), "anything");
    }
}
