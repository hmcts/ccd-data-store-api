package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SupplementaryDataOperationTest {

    SupplementaryDataOperation operation;

    @Test
    void getOperationName() {
        operation = SupplementaryDataOperation.SET;
        assertEquals("$set", operation.getOperationName());
        operation = SupplementaryDataOperation.INC;
        assertEquals("$inc", operation.getOperationName());
        operation = SupplementaryDataOperation.FIND;
        assertEquals("$find", operation.getOperationName());
    }

    @Test
    void getOperation() {
        Optional<SupplementaryDataOperation> operation = SupplementaryDataOperation.getOperation("$set");
        assertEquals(SupplementaryDataOperation.SET, operation.get());

        operation = SupplementaryDataOperation.getOperation("$inc");
        assertEquals(SupplementaryDataOperation.INC, operation.get());

        operation = SupplementaryDataOperation.getOperation("$find");
        assertEquals(SupplementaryDataOperation.FIND, operation.get());
    }
}
