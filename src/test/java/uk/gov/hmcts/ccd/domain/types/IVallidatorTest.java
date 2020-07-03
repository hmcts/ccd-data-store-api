package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

/**
 * Groups common code used in ValidatorTest.
 */
@SuppressWarnings("PMD.ConstantsInInterface")
interface IVallidatorTest {
    JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;
    ObjectMapper MAPPER = new ObjectMapper();
}
