package uk.gov.hmcts.jsonstore;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import uk.gov.hmcts.ccd.fta.util.ReflectionUtils;

public abstract class JsonStoreWithInheritance {

    private static final String INHERITANCE_APPLIED = "inheritanceApplied";
    protected JsonNode rootNode;
    protected Map<String, JsonNode> nodeLibrary = new HashMap<>();
    protected Map<Class<?>, Map<String, ?>> objectLibraryPerTypes = new HashMap<>();
    protected final String idFieldName;
    protected final String inheritanceFieldName;

    public JsonStoreWithInheritance() {
        this("_guid_", "_extends_");
    }

    public JsonStoreWithInheritance(String idFieldName, String inheritanceFieldName) {
        this.idFieldName = idFieldName;
        this.inheritanceFieldName = inheritanceFieldName;
    }

    protected JsonNode getRootNode() throws Exception {
        if (rootNode == null)
            loadStore();
        return rootNode;
    }

    protected Map<String, JsonNode> getNodeLibrary() {
        if (rootNode == null)
            loadStore();
        return nodeLibrary;
    }

    private void loadStore() {
        try {
            buildObjectStore();
            addToLibrary(rootNode);
            for (String id : nodeLibrary.keySet())
                overwriteInheritedValuesOf(nodeLibrary.get(id));
            removeInheritanceMechanismFields(rootNode);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getMapWithIds(Class<? extends T> clazz) throws Exception {
        Map<String, T> objectLibrary = (Map<String, T>) objectLibraryPerTypes.get(clazz);
        if (objectLibrary == null) {
            objectLibrary = new HashMap<String, T>();
            Set<String> keys = getNodeLibrary().keySet();
            ObjectMapper om = new ObjectMapper();
            om.enable(SerializationFeature.INDENT_OUTPUT);
            om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            for (String key : keys) {
                JsonNode nodeInLibrary = nodeLibrary.get(key);
                String jsonText = om.writeValueAsString(nodeInLibrary);
                T anOnject = om.readValue(jsonText, clazz);
                try {
                    if (ReflectionUtils.retrieveFieldInObject(anOnject, idFieldName) != null) {
                        objectLibrary.put(key, anOnject);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            objectLibraryPerTypes.put(clazz, objectLibrary);
        }
        return objectLibrary;
    }

    public <T> T getObjectWithId(String id, Class<? extends T> clazz) throws Exception {
        return getMapWithIds(clazz).get(id);
    }

    private void removeInheritanceMechanismFields(JsonNode node) {
        if (node.has(INHERITANCE_APPLIED)) {
            ((ObjectNode) node).remove(INHERITANCE_APPLIED);
        }
        if (node.has(inheritanceFieldName)) {
            ((ObjectNode) node).remove(inheritanceFieldName);
        }

        Iterator<JsonNode> fields = node.iterator();
        while (fields.hasNext())
            removeInheritanceMechanismFields(fields.next());
    }

    protected abstract void buildObjectStore() throws Exception;

    private void overwriteInheritedValuesOf(JsonNode object) {
        if (!shouldApplyInheritanceOn(object))
            return;
        JsonNode parentIdField = object.get(inheritanceFieldName);
        if (parentIdField != null) {
            String parentId = parentIdField.asText();
            JsonNode parentNode = nodeLibrary.get(parentId);
            throwExceptionIfParentNotFound(object, parentNode, parentId);
            overwriteInheritedValuesOf(parentNode);
            Iterator<String> parentIterator = parentNode.fieldNames();
            while (parentIterator.hasNext()) {
                String fieldNameInParent = parentIterator.next();
                if (!isInheritanceMechanismField(fieldNameInParent)) {
                    overwriteInheritedValuesOf(parentNode.get(fieldNameInParent));
                    JsonNode parentFieldCopy = parentNode.get(fieldNameInParent).deepCopy();
                    if (object.has(fieldNameInParent)) {
                        JsonNode thisField = object.get(fieldNameInParent);
                        if (thisField.isContainerNode()) {
                            overwriteInheritedValuesOf(thisField);
                            ((ObjectNode) object).set(fieldNameInParent, parentFieldCopy);
                            underlayFor(parentFieldCopy, thisField);
                        }
                    } else {
                        ((ObjectNode) object).set(fieldNameInParent, parentFieldCopy);
                    }
                }
            }
        }

        Iterator<JsonNode> fields = object.iterator();
        while (fields.hasNext())
            overwriteInheritedValuesOf(fields.next());

        if (object instanceof ObjectNode)
            ((ObjectNode) object).set(INHERITANCE_APPLIED, BooleanNode.TRUE);
    }

    private boolean shouldApplyInheritanceOn(JsonNode object) {
        if (object == null || !object.isContainerNode())
            return false;
        if (object.has(INHERITANCE_APPLIED))
            return false;
        return true;
    }

    boolean containsAnyMechanismFields(JsonNode object) {
        return object.has(idFieldName) || object.has(inheritanceFieldName) || object.has(INHERITANCE_APPLIED);
    }

    private void throwExceptionIfParentNotFound(JsonNode object, JsonNode parentNode, String parentId) {
        if (parentNode == null) {
            String idPart = "an object without a " + idFieldName + " value specified";
            if (object.has(idFieldName)) {
                idPart = object.get(idFieldName).asText();
            }
            throw new RuntimeException("Parent object with key " + parentId + " not found for " + idPart + ".");
        }
    }

    private void underlayFor(JsonNode under, JsonNode over) {
        if (over.isArray()) {
        } else {
            Iterator<String> overFields = over.fieldNames();
            while (overFields.hasNext()) {
                String subfieldName = overFields.next();
                if (!isInheritanceMechanismField(subfieldName)) {
                    JsonNode overField = over.get(subfieldName);
                    JsonNode underField = under.get(subfieldName);
                    if (underField != null && underField.isContainerNode()) {
                        underlayFor(underField, overField);
                    } else {
                        ((ObjectNode) under).set(subfieldName, overField);
                    }
                }
            }
        }
    }

    private boolean isInheritanceMechanismField(String fieldName) {
        return fieldName.equalsIgnoreCase(idFieldName) || fieldName.equalsIgnoreCase(inheritanceFieldName)
                || fieldName.equalsIgnoreCase(INHERITANCE_APPLIED);
    }

    private void addToLibrary(JsonNode object) throws Exception {
        String keyFromIdField;
        if (shouldPlaceInLibrary(object)) {
            if (object.has(idFieldName)) {
                keyFromIdField = object.get(idFieldName).asText();
            } else {
                keyFromIdField = UUID.randomUUID().toString();
            }
            nodeLibrary.put(keyFromIdField, object);
        }
        Iterator<JsonNode> iterator = object.iterator();
        while (iterator.hasNext()) {
            addToLibrary(iterator.next());
        }
    }

    private boolean shouldPlaceInLibrary(JsonNode object) {
        return object.has(idFieldName) || object.has(inheritanceFieldName);
    }
}
