package uk.gov.hmcts.ccd.domain.types;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * Groups common code used in ValidatorTest.
 */
interface IVallidatorTest {
    static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;
    static final ObjectMapper MAPPER = JsonMapper.builderWithJackson2Defaults().build();
}
