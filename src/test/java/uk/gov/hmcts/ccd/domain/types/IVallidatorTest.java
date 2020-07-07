package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

/**
 * Groups common code used in ValidatorTest.
 */
interface IVallidatorTest {
    static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;
    static final ObjectMapper MAPPER = new ObjectMapper();
}
