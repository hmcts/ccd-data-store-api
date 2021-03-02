package uk.gov.hmcts.ccd.domain.types.sanitiser;

import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

public class CollectionSanitiserTest {

    private static final JsonNodeFactory JSON_FACTORY = new JsonNodeFactory(false);

    private static final String TYPE_1 = "Type1";
    private static final FieldTypeDefinition FIELD_1_TYPE = new FieldTypeDefinition();

    private static final JsonNode ITEM_1_VALUE_INITIAL = JSON_FACTORY.textNode("Initial value 1");
    private static final JsonNode ITEM_1_VALUE_SANITISED = JSON_FACTORY.textNode("Sanitised value 1");
    private static final JsonNode ITEM_2_VALUE_INITIAL = JSON_FACTORY.textNode("Initial value 2");
    private static final JsonNode ITEM_2_VALUE_SANITISED = JSON_FACTORY.textNode("Sanitised value 2");

    private static final FieldTypeDefinition COLLECTION_FIELD_TYPE = new FieldTypeDefinition();
    private static final JsonNode UNIQUE_ID = JSON_FACTORY.textNode("123");

    static {
        FIELD_1_TYPE.setId(TYPE_1);
        FIELD_1_TYPE.setType(TYPE_1);

        COLLECTION_FIELD_TYPE.setCollectionFieldTypeDefinition(FIELD_1_TYPE);
    }

    private Sanitiser type1Sanitiser;

    private CollectionSanitiser collectionSanitiser;

    @Before
    public void setUp() throws Exception {

        type1Sanitiser = mock(Sanitiser.class);
        doReturn(TYPE_1).when(type1Sanitiser).getType();

        collectionSanitiser = new CollectionSanitiser();
        collectionSanitiser.setSanitisers(Collections.singletonList(type1Sanitiser));
    }

    @Test
    public void getType() {
        assertThat(collectionSanitiser.getType(), equalTo(COLLECTION));
    }

    @Test
    public void shouldSanitiseEachCollectionItem() {
        doReturn(ITEM_1_VALUE_SANITISED).when(type1Sanitiser).sanitise(FIELD_1_TYPE, ITEM_1_VALUE_INITIAL);
        doReturn(ITEM_2_VALUE_SANITISED).when(type1Sanitiser).sanitise(FIELD_1_TYPE, ITEM_2_VALUE_INITIAL);

        final ArrayNode collectionData = JSON_FACTORY.arrayNode();
        collectionData
            .add(JSON_FACTORY.objectNode().set(CollectionSanitiser.VALUE, ITEM_1_VALUE_INITIAL))
            .add(JSON_FACTORY.objectNode().set(CollectionSanitiser.VALUE, ITEM_2_VALUE_INITIAL));

        final JsonNode sanitisedData = collectionSanitiser.sanitise(COLLECTION_FIELD_TYPE, collectionData);

        verify(type1Sanitiser, times(1)).sanitise(FIELD_1_TYPE, ITEM_1_VALUE_INITIAL);
        verify(type1Sanitiser, times(1)).sanitise(FIELD_1_TYPE, ITEM_2_VALUE_INITIAL);
        assertThat(sanitisedData.get(0).get(CollectionSanitiser.VALUE), is(ITEM_1_VALUE_SANITISED));
        assertThat(sanitisedData.get(1).get(CollectionSanitiser.VALUE), is(ITEM_2_VALUE_SANITISED));
    }

    @Test
    public void shouldGenerateIDsForItemsWithoutID() {
        final ArrayNode collectionData = JSON_FACTORY.arrayNode();
        collectionData
            .add(JSON_FACTORY.objectNode().set(CollectionSanitiser.VALUE, ITEM_1_VALUE_INITIAL))
            .add(JSON_FACTORY.objectNode().set(CollectionSanitiser.VALUE, ITEM_2_VALUE_INITIAL));

        final JsonNode sanitisedData = collectionSanitiser.sanitise(COLLECTION_FIELD_TYPE, collectionData);

        assertThat(sanitisedData.get(0).get(CollectionSanitiser.ID), is(notNullValue()));
        assertThat(sanitisedData.get(1).get(CollectionSanitiser.ID), is(notNullValue()));
        assertThat(sanitisedData.get(1).get(CollectionSanitiser.ID), not(equalTo(sanitisedData.get(0)
                .get(CollectionSanitiser.ID))));
    }

    @Test
    public void shouldKeepExistingIDsUnaltered() {
        final ArrayNode collectionData = JSON_FACTORY.arrayNode();
        final ObjectNode itemNode = JSON_FACTORY.objectNode();
        itemNode.set(CollectionSanitiser.VALUE, ITEM_1_VALUE_INITIAL);
        itemNode.set(CollectionSanitiser.ID, UNIQUE_ID);
        collectionData.add(itemNode);

        final JsonNode sanitisedData = collectionSanitiser.sanitise(COLLECTION_FIELD_TYPE, collectionData);

        assertThat(sanitisedData.get(0).get(CollectionSanitiser.ID), is(UNIQUE_ID));
    }

    @Test
    public void shouldIgnoreItemWithoutValue() {

        final ArrayNode collectionData = JSON_FACTORY.arrayNode();
        collectionData
            .add(JSON_FACTORY.objectNode().set("NoValue", ITEM_1_VALUE_INITIAL));

        final JsonNode sanitisedData = collectionSanitiser.sanitise(COLLECTION_FIELD_TYPE, collectionData);

        verify(type1Sanitiser, never()).sanitise(FIELD_1_TYPE, ITEM_1_VALUE_INITIAL);
        assertThat(sanitisedData.size(), is(0));
    }

}
