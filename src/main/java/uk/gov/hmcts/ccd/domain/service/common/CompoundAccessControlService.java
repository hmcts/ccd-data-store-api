package uk.gov.hmcts.ccd.domain.service.common;

import static java.util.Spliterators.spliteratorUnknownSize;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_DELETE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_UPDATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.hasAccessControlList;

import uk.gov.hmcts.ccd.domain.model.definition.CaseField;

import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CompoundAccessControlService {

    private static final Logger LOG = LoggerFactory.getLogger(CompoundAccessControlService.class);

    public boolean hasAccessForAction(final JsonNode newData, final JsonNode existingData, final CaseField caseField, final Set<String> userRoles) {
        if (!itemAddedAndHasCreateAccess(newData, caseField, userRoles)) {
            return false;
        }
        if (!itemDeletedAndHasDeleteAccess(existingData, newData, caseField, userRoles)) {
            return false;
        }
        return itemUpdatedAndHasUpdateAccess(existingData, newData, caseField, userRoles);
    }

    private boolean itemAddedAndHasCreateAccess(JsonNode newData, CaseField caseField, Set<String> userRoles) {
        if (caseField.isCollectionFieldType()) {
            final JsonNode jsonNode = newData.get(caseField.getId());
            boolean containsNewItem = containsNewCollectionItem(jsonNode);
            return (!containsNewItem || hasAccessControlList(userRoles, CAN_CREATE, caseField.getAccessControlLists()))
                && !isCreateDeniedForChildren(jsonNode, caseField, userRoles);
        } else {
            return !isCreateDeniedForChildren(newData.get(caseField.getId()), caseField, userRoles);
        }
    }

    private boolean isCreateDeniedForChildren(JsonNode data, CaseField caseField, Set<String> userRoles) {
        if (caseField.isCollectionFieldType() && containsNewCollectionItem(data)
            && !hasAccessControlList(userRoles, CAN_CREATE, caseField.getAccessControlLists())) {
            return true;
        }
        boolean denied = false;
        if (caseField.isCollectionFieldType() && data.size() > 0) {
            for (JsonNode node : data) {
                for (CaseField field : caseField.getFieldType().getCollectionFieldType().getComplexFields()) {
                    if (field.isCompound() && node.get("value") != null && node.get("value").get(field.getId()) != null
                        && isCreateDeniedForChildren(node.get("value").get(field.getId()), field, userRoles)) {
                        LOG.info("Child {} of {} has new data and no create access", field.getId(), caseField.getId());
                        denied = true;
                        break;
                    }
                }
                if (denied)
                    break;
            }
        } else if (caseField.isComplexFieldType() && data != null) {
            for (CaseField field : caseField.getFieldType().getComplexFields()) {
                if (field.isCompound() && isCreateDeniedForChildren(data.get(field.getId()), field, userRoles)) {
                    LOG.info("Child {} has new data and no create access", field.getId());
                    denied = true;
                    break;
                }
            }
        }
        return denied;
    }

    private boolean itemDeletedAndHasDeleteAccess(JsonNode existingData, JsonNode newData, CaseField caseField, Set<String> userRoles) {
        boolean containsDeletedItem = caseField.isCollectionFieldType() && StreamSupport
            .stream(spliteratorUnknownSize(existingData.get(caseField.getId()).elements(), Spliterator.ORDERED), false)
            .anyMatch(oldItem -> itemMissing(oldItem, newData.get(caseField.getId())));

        return (!containsDeletedItem || hasAccessControlList(userRoles, CAN_DELETE, caseField.getAccessControlLists()))
            && !isDeleteDeniedForChildren(existingData.get(caseField.getId()), newData.get(caseField.getId()), caseField, userRoles);
    }

    private boolean itemMissing(JsonNode oldItem, JsonNode newValue) {
        boolean itemExists = StreamSupport.stream(spliteratorUnknownSize(newValue.elements(), Spliterator.ORDERED), false)
            .anyMatch(newItem -> !isNullId(newItem) && newItem.get("id").equals(oldItem.get("id")));
        return !itemExists;
    }

    private boolean isDeleteDeniedForChildren(final JsonNode existingData, final JsonNode newData, final CaseField caseField, final Set<String> userRoles) {
        boolean denied = false;
        if (caseField.isCollectionFieldType() && existingData.size() > 0) {
            for (JsonNode existingNode : existingData) {
                Optional<JsonNode> optionalNewNode = findCorrespondingNode(newData, existingNode.get("id"));
                if (optionalNewNode.isPresent()) {
                    JsonNode newNode = optionalNewNode.get();
                    for (CaseField field : caseField.getFieldType().getCollectionFieldType().getComplexFields()) {
                        if (field.isCompound() && existingNode.get("value") != null && existingNode.get("value").get(field.getId()) != null
                            && isDeleteDeniedForChildren(existingNode.get("value").get(field.getId()), newNode.get("value").get(field.getId()), field, userRoles)) {
                            denied = true;
                            LOG.info("Simple child {} of {} has been deleted item but no Delete ACL", field.getId(), caseField.getId());
                            break;
                        }
                    }
                } else {
                    if (!hasAccessControlList(userRoles, CAN_DELETE, caseField.getAccessControlLists())) {
                        LOG.info("A child {} item has been deleted item but no Delete ACL", caseField.getId());
                        denied = true;
                    }
                }
                if (denied)
                    break;
            }
        } else {
            if (existingData.isObject() && existingData.size() > 0 && newData.isObject() && newData.size() > 0) {
                for (CaseField field : caseField.getFieldType().getComplexFields()) {
                    if (field.isCompound() && existingData.get(field.getId()) != null && newData.get(field.getId()) != null
                        && isDeleteDeniedForChildren(existingData.get(field.getId()), newData.get(field.getId()), field, userRoles)) {
                        denied = true;
                        LOG.info("A child {} of {} has data update without Update ACL", field.getId(), caseField.getId());
                        break;
                    }
                }
            } else {
                LOG.info("Weird to see caseField {} has ArrayNode data", caseField);
            }
        }
        return denied;
    }

    private boolean itemUpdatedAndHasUpdateAccess(JsonNode existingData, JsonNode newData, CaseField caseField, Set<String> userRoles) {
        if (caseField.isCollectionFieldType()) {
            boolean containsUpdatedItem = StreamSupport
                .stream(spliteratorUnknownSize(existingData.get(caseField.getId()).elements(), Spliterator.ORDERED), false)
                .anyMatch(oldItem -> itemUpdated(oldItem, newData.get(caseField.getId()), caseField, userRoles));

            return !containsUpdatedItem; // || hasAccessControlList(userRoles, CAN_UPDATE, caseField.getAccessControlLists());
        } else {
            return !isUpdateDenied(existingData.get(caseField.getId()), newData.get(caseField.getId()), caseField, userRoles);
        }
    }

    private boolean itemUpdated(JsonNode oldItem, JsonNode newValue, CaseField caseField, Set<String> userRoles) {
        return StreamSupport.stream(spliteratorUnknownSize(newValue.elements(), Spliterator.ORDERED), false)
            .anyMatch(newItem -> {
                boolean itemExists = !isNullId(newItem) && newItem.get("id").equals(oldItem.get("id"));
                if (itemExists) {
                    return !newItem.equals(oldItem) && isUpdateDenied(oldItem, newItem, caseField, userRoles);
                }
                return false;
            });
    }

    private boolean isUpdateDenied(final JsonNode oldItem, final JsonNode newItem, final CaseField caseField, Set<String> userRoles) {
        boolean denied = false;
        if (caseField.isCollectionFieldType()) {
            if (oldItem.isObject() && oldItem.get("value") != null && newItem.isObject() && newItem.get("value") != null) {
                denied = checkCollectionNodesForUpdate(caseField, oldItem, newItem, userRoles);
            } else if (oldItem.isArray() && oldItem.size() > 0 && newItem.isArray() && newItem.size() > 0) {
                for (JsonNode oldNode : oldItem) {
                    Optional<JsonNode> optionalNewNode = findCorrespondingNode(newItem, oldNode.get("id"));
                    if (optionalNewNode.isPresent()) {
                        JsonNode newNode = optionalNewNode.get();
                        denied = checkCollectionNodesForUpdate(caseField, oldNode, newNode, userRoles);
                    } else {
                        LOG.info("Data for item with id {} of type {} has been deleted", oldNode.get("id"), caseField.getId());
                    }
                    if (denied)
                        break;
                }
            }
        } else {
            if (oldItem.isObject() && oldItem.size() > 0 && newItem.isObject() && newItem.size() > 0) {
                denied = checkComplexNodesForUpdate(caseField, oldItem, newItem, userRoles);
            } else {
                LOG.info("Weird to see casefield {} has ArrayNode data", caseField);
            }
        }
        return denied;
    }

    private boolean checkCollectionNodesForUpdate(final CaseField caseField, final JsonNode oldNode, final JsonNode newNode, Set<String> userRoles) {
        boolean denied = false;
        for (CaseField field : caseField.getFieldType().getCollectionFieldType().getComplexFields()) {
            if (!field.isCompound() && oldNode.get("value").get(field.getId()) != null
                && !oldNode.get("value").get(field.getId()).equals(newNode.get("value").get(field.getId()))
                && !hasAccessControlList(userRoles, CAN_UPDATE, field.getAccessControlLists())) {
                denied = true;
                LOG.info("Simple child {} of {} has data update but no Update ACL", field.getId(), caseField.getId());
                break;

            } else if (field.isCompound() && oldNode.get("value").get(field.getId()) != null && newNode.get("value").get(field.getId()) != null
                && isUpdateDenied(oldNode.get("value").get(field.getId()), newNode.get("value").get(field.getId()), field, userRoles)) {
                denied = true;
                LOG.info("A child {} of {} has data update without Update ACL", field.getId(), caseField.getId());
                break;
            }
        }
        return denied;
    }

    private boolean checkComplexNodesForUpdate(final CaseField caseField, final JsonNode oldNode, final JsonNode newNode, Set<String> userRoles) {
        boolean denied = false;
        for (CaseField field : caseField.getFieldType().getComplexFields()) {
            if (!field.isCompound() && !oldNode.get(field.getId()).equals(newNode.get(field.getId()))
                && !hasAccessControlList(userRoles, CAN_UPDATE, field.getAccessControlLists())) {
                denied = true;
                LOG.info("Simple child {} of {} has data update but no Update ACL", field.getId(), caseField.getId());
                break;

            } else if (field.isCompound() && oldNode.get(field.getId()) != null && newNode.get(field.getId()) != null
                && isUpdateDenied(oldNode.get(field.getId()), newNode.get(field.getId()), field, userRoles)) {
                denied = true;
                LOG.info("A child {} of {} has data update without Update ACL", field.getId(), caseField.getId());
                break;
            }
        }
        return denied;
    }

    private Optional<JsonNode> findCorrespondingNode(final JsonNode newItem, final JsonNode id) {
        if (newItem == null) {
            return Optional.empty();
        }
        return StreamSupport.stream(spliteratorUnknownSize(newItem.elements(), Spliterator.ORDERED), false)
            .filter(node -> node.get("id") != null && node.get("id").equals(id))
            .findFirst();
    }

//    private boolean hasAccessForCollectionItemAction(JsonNode newData, JsonNode existingData, String newFieldName, List<CaseField> caseFieldDefinitions, Set<String> userRoles) {
//        if (!itemAddedAndHasCreateAccess(newData, newFieldName, caseFieldDefinitions, userRoles)) {
//        return false;
//        }
//        if (!itemDeletedAndHasDeleteAccess(existingData, newData, newFieldName, caseFieldDefinitions, userRoles)) {
//            return false;
//        }
//        return itemUpdatedAndHasUpdateAccess(existingData, newData, newFieldName, caseFieldDefinitions, userRoles);
//    }

//    private boolean itemAddedAndHasCreateAccess(JsonNode newData, String newFieldName, List<CaseField> caseFieldDefinitions, Set<String> userRoles) {
//        JsonNode newValue = newData.get(newFieldName);
//        boolean containsNewItem = StreamSupport.stream(spliteratorUnknownSize(newValue.elements(), Spliterator.ORDERED), false)
//            .anyMatch(this::isNullId);
//        return !containsNewItem || hasCaseFieldAccess(caseFieldDefinitions, userRoles, CAN_CREATE, newFieldName);
//    }

    private boolean containsNewCollectionItem(JsonNode data) {
        return StreamSupport.stream(spliteratorUnknownSize(data.elements(), Spliterator.ORDERED), false)
            .anyMatch(this::isNullId);
    }

    private boolean isNullId(JsonNode newItem) {
        return newItem.get("id") == null
            || newItem.get("id").equals(NullNode.getInstance())
            || newItem.get("id").asText().equalsIgnoreCase("null");
    }
}
