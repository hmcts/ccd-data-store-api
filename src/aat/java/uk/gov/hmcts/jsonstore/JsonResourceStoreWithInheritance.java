package uk.gov.hmcts.jsonstore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class JsonResourceStoreWithInheritance extends JsonStoreWithInheritance {
    private String[] resourcePaths;

    public JsonResourceStoreWithInheritance(String[] resourcePaths) {
        super();
        this.resourcePaths = resourcePaths;
    }

    public JsonResourceStoreWithInheritance(String[] resourcePaths, String idFieldName, String inheritanceFieldName) {
        super(idFieldName, inheritanceFieldName);
        this.resourcePaths = resourcePaths;
    }

    @Override
    protected void buildObjectStore() throws Exception {
        rootNode = buildObjectStoreInResourcePaths();
    }

    private JsonNode buildObjectStoreInResourcePaths() throws Exception {
        ArrayNode store = new ArrayNode(null);
        for (String resource : resourcePaths) {
            JsonNode substore = null;
            if (resource.toLowerCase().endsWith(".json"))
                substore = buildObjectStoreInAResource(resource);

            if (substore != null) {
                if (substore.isArray()) {
                    for (int i = 0; i < substore.size(); i++)
                        store.add(substore.get(i));
                } else
                    store.add(substore);
            }
        }
        if (store.size() == 1)
            return store.get(0);
        return store;
    }

    private JsonNode buildObjectStoreInAResource(String resource) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(this.getClass().getClassLoader().getResourceAsStream(resource));
    }

}
