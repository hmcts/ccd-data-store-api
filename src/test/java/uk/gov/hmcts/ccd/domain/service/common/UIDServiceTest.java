package uk.gov.hmcts.ccd.domain.service.common;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UIDServiceTest {

    private UIDService uidService = new UIDService();

    @Test
    public void shouldReturnFalseForNullUID() throws Exception {
        String uid = null;

        assertFalse(uidService.validateUID(uid));
    }

    @Test
    public void shouldReturnFalseForInvalidLengthUID() throws Exception {
        String uid = "1";

        assertFalse(uidService.validateUID(uid));
    }

    @Test
    public void shouldReturnFalseForInvalidContentUID() throws Exception {
        String uid = "abcdefghijklmnop";

        assertFalse(uidService.validateUID(uid));
    }

    @Test
    public void shouldGenerateValid16digitUID() throws Exception {
        String uid = uidService.generateUID();

        assertTrue(uid.length() == 16);
    }

    @Test
    public void shouldValidateGeneratedUID() throws Exception {
        String uid = uidService.generateUID();

        assertTrue(uidService.validateUID(uid));
    }

}
