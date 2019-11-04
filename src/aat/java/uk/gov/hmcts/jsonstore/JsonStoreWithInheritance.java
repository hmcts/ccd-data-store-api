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

public abstract class JsonStoreWithInheritance {

	protected JsonNode rootNode;
	protected Map<String, JsonNode> nodeLibrary = new HashMap<>();
	protected Map<Class<?>, Map<String, ?>> objectLibraryPerTypes = new HashMap<>();
	protected final String idFieldName;
	protected final String inheritanceFieldName;

	public JsonStoreWithInheritance() {
		this("guid_", "extends_");
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
			removeInheritanceAppliedFields(rootNode);
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
					if (ReflectionUtils.retrieveFieldInObject(anOnject, idFieldName) != null)
						objectLibrary.put(key, anOnject);
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

	private void removeInheritanceAppliedFields(JsonNode node) {
		if (node.has("inheritanceApplied"))
			((ObjectNode) node).remove("inheritanceApplied");

		Iterator<JsonNode> fields = node.iterator();
		while (fields.hasNext())
			removeInheritanceAppliedFields(fields.next());
	}

	protected abstract void buildObjectStore() throws Exception;

	private void overwriteInheritedValuesOf(JsonNode object) {
		if (object == null || !object.isContainerNode())
			return;
		if (object.has("inheritanceApplied"))
			return;
		JsonNode parentIdField = object.get(inheritanceFieldName);
		if (parentIdField != null) {
			String parentId = parentIdField.asText();
			JsonNode parentNode = nodeLibrary.get(parentId);
			if (parentNode == null)
				throw new RuntimeException("Parent node not found with key " + parentId);
			overwriteInheritedValuesOf(parentNode);
			Iterator<String> parentIterator = parentNode.fieldNames();
			while (parentIterator.hasNext()) {
				String fieldName = parentIterator.next();
				if (!fieldName.equalsIgnoreCase(idFieldName) && !fieldName.equalsIgnoreCase(inheritanceFieldName)
						&& !fieldName.equalsIgnoreCase("inheritanceApplied")) {
					overwriteInheritedValuesOf(parentNode.get(fieldName));
					JsonNode parentFieldCopy = parentNode.get(fieldName).deepCopy();
					if (object.has(fieldName)) {
						JsonNode thisField = object.get(fieldName);
						if (thisField.isContainerNode()) {
							overwriteInheritedValuesOf(thisField);
							((ObjectNode) object).set(fieldName, parentFieldCopy);
							underlayFor(parentFieldCopy, thisField);
						}
					} else {
						((ObjectNode) object).set(fieldName, parentFieldCopy);
					}
				}
			}
		} else {
			Iterator<JsonNode> fields = object.iterator();
			while (fields.hasNext())
				overwriteInheritedValuesOf(fields.next());
		}

		if (object instanceof ObjectNode)
			((ObjectNode) object).set("inheritanceApplied", BooleanNode.TRUE);
	}

	private void underlayFor(JsonNode under, JsonNode over) {
		if (over.isArray()) {
		} else {
			Iterator<String> overFields = over.fieldNames();
			while (overFields.hasNext()) {
				String subbfieldName = overFields.next();
				JsonNode overField = over.get(subbfieldName);
				JsonNode underField = under.get(subbfieldName);
				if (underField != null && underField.isContainerNode()) {
					underlayFor(underField, overField);
				} else {
					((ObjectNode) under).set(subbfieldName, overField);
				}
			}
		}
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
