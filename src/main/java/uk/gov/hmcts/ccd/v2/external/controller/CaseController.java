package uk.gov.hmcts.ccd.v2.external.controller;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ExampleProperty;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.auditlog.LogAudit;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.ccd.domain.model.std.validator.SupplementaryDataUpdateRequestValidator;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.createcase.CreateCaseOperation;
import uk.gov.hmcts.ccd.domain.service.createevent.CreateEventOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getevents.GetEventsOperation;
import uk.gov.hmcts.ccd.domain.service.supplementarydata.SupplementaryDataUpdateOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.resource.CaseEventsResource;
import uk.gov.hmcts.ccd.v2.external.resource.CaseResource;
import uk.gov.hmcts.ccd.v2.external.resource.SupplementaryDataResource;

import static org.springframework.http.ResponseEntity.status;
import static uk.gov.hmcts.ccd.auditlog.AuditOperationType.CASE_ACCESSED;
import static uk.gov.hmcts.ccd.auditlog.AuditOperationType.CREATE_CASE;
import static uk.gov.hmcts.ccd.auditlog.AuditOperationType.UPDATE_CASE;

@RestController
@RequestMapping(path = "/")
public class CaseController {
    private final GetCaseOperation getCaseOperation;
    private final CreateEventOperation createEventOperation;
    private final CreateCaseOperation createCaseOperation;
    private final UIDService caseReferenceService;
    private final GetEventsOperation getEventsOperation;
    private final SupplementaryDataUpdateOperation supplementaryDataUpdateOperation;
    private final SupplementaryDataUpdateRequestValidator requestValidator;

    @Autowired
    public CaseController(
        @Qualifier(CreatorGetCaseOperation.QUALIFIER) final GetCaseOperation getCaseOperation,
        @Qualifier("authorised") final CreateEventOperation createEventOperation,
        @Qualifier("authorised") final CreateCaseOperation createCaseOperation,
        UIDService caseReferenceService,
        @Qualifier("authorised") GetEventsOperation getEventsOperation,
        @Qualifier("authorised") SupplementaryDataUpdateOperation supplementaryDataUpdateOperation,
        SupplementaryDataUpdateRequestValidator requestValidator
    ) {
        this.getCaseOperation = getCaseOperation;
        this.createEventOperation = createEventOperation;
        this.createCaseOperation = createCaseOperation;
        this.caseReferenceService = caseReferenceService;
        this.getEventsOperation = getEventsOperation;
        this.supplementaryDataUpdateOperation = supplementaryDataUpdateOperation;
        this.requestValidator = requestValidator;
    }

    @Transactional
    @GetMapping(
        path = "/cases/{caseId}",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.CASE
        }
    )
    @ApiOperation(
        value = "Retrieve a case by ID",
        notes = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success",
            response = CaseResource.class
        ),
        @ApiResponse(
            code = 400,
            message = V2.Error.CASE_ID_INVALID
        ),
        @ApiResponse(
            code = 404,
            message = V2.Error.CASE_NOT_FOUND
        )
    })
    @LogAudit(operationType = CASE_ACCESSED, caseId = "#caseId",
        jurisdiction = "#result.body.jurisdiction", caseType = "#result.body.caseType")
    public ResponseEntity<CaseResource> getCase(@PathVariable("caseId") String caseId) {
        if (!caseReferenceService.validateUID(caseId)) {
            throw new BadRequestException(V2.Error.CASE_ID_INVALID);
        }

        final CaseDetails caseDetails = this.getCaseOperation.execute(caseId)
                                                             .orElseThrow(() -> new CaseNotFoundException(caseId));

        return ResponseEntity.ok(new CaseResource(caseDetails));
    }

    @Transactional
    @PostMapping(
        path = "/cases/{caseId}/events",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.CREATE_EVENT
        }
    )
    @ApiOperation(
        value = "Submit an event for a case"
    )
    @ApiImplicitParams({
        @ApiImplicitParam(name = V2.EXPERIMENTAL_HEADER, value = "'true' to use this endpoint", paramType = "header")
    })
    @ApiResponses({
        @ApiResponse(
            code = 201,
            message = "Event has been created successfully",
            response = CaseResource.class
        ),
        @ApiResponse(
            code = 400,
            message = V2.Error.CASE_ID_INVALID
        ),
        @ApiResponse(
            code = 400,
            message = V2.Error.EVENT_TRIGGER_NOT_FOUND
        ),
        @ApiResponse(
            code = 404,
            message = V2.Error.CASE_NOT_FOUND
        ),
        @ApiResponse(
            code = 422,
            message = "The event could not be processed, for example due to one of the following:\n"
                + "- Data validation failed\n"
                + "- Invalid event (e.g. event ID not provided/known)\n"
                + "- Case does not comply with pre-state condition for event\n"
        ),
        @ApiResponse(
            code = 504,
            message = V2.Error.CALLBACK_EXCEPTION
        )
    })
    @ResponseStatus(HttpStatus.CREATED) // To remove default 200 response from Swagger
    @LogAudit(operationType = UPDATE_CASE, caseId = "#caseId", jurisdiction = "#result.body.jurisdiction",
        caseType = "#result.body.caseType", eventName = "#content.event.eventId")
    public ResponseEntity<CaseResource> createEvent(@ApiParam(value = "Case ID for which the event is being submitted",
                                                    required = true)
                                                    @PathVariable("caseId") String caseId,
                                                    @ApiParam(value = "Case data content for the event. Note that the "
                                                        + "`data` property "
                                                        + "is used for event submission data; NOT the `event_data`. "
                                                        + "For example:\n"
                                                        + "```\n"
                                                        + "{\n"
                                                        + "    \"data\": {\n"
                                                        + "        \"TextField\": \"TextField1\",\n"
                                                        + "        \"NumberField\": \"123\",\n"
                                                        + "        \"YesOrNoField\": \"Yes\",\n"
                                                        + "        \"PhoneUKField\": \"01234 567890\",\n"
                                                        + "        \"EmailField\": \"email@gmail.com\",\n"
                                                        + "        \"MoneyGBPField\": \"12300\",\n"
                                                        + "        \"DateField\": \"2015-12-23\",\n"
                                                        + "        \"DateTimeField\": \"2000-05-17T12:30:00.000\",\n"
                                                        + "        \"FixedListField\": \"VALUE3\",\n"
                                                        + "        \"MultiSelectListField\": [\n"
                                                        + "            \"OPTION5\",\n"
                                                        + "            \"OPTION4\"\n"
                                                        + "        ],\n"
                                                        + "        \"ComplexField\": {\n"
                                                        + "            \"ComplexTextField\": \"Nested text field\",\n"
                                                        + "            \"ComplexFixedListField\": null,\n"
                                                        + "            \"ComplexNestedField\": {\n"
                                                        + "                \"NestedNumberField\": \"987\",\n"
                                                        + "                \"NestedCollectionTextField\": []\n"
                                                        + "            }\n"
                                                        + "        },\n"
                                                        + "        \"CollectionField\": [\n"
                                                        + "            {\n"
                                                        + "                \"id\": null,\n"
                                                        + "                \"value\": \"Collection field 1\"\n"
                                                        + "            },\n"
                                                        + "            {\n"
                                                        + "                \"id\": null,\n"
                                                        + "                \"value\": \"Collection field 2\"\n"
                                                        + "            }\n"
                                                        + "        ],\n"
                                                        + "        \"AddressUKField\": {\n"
                                                        + "            \"AddressLine1\": \"1 The Street\",\n"
                                                        + "            \"AddressLine2\": \"\",\n"
                                                        + "            \"AddressLine3\": \"\",\n"
                                                        + "            \"PostTown\": \"Town\",\n"
                                                        + "            \"County\": \"County\",\n"
                                                        + "            \"PostCode\": \"AB1 2CD\",\n"
                                                        + "            \"Country\": \"England\"\n"
                                                        + "        }\n"
                                                        + "    },\n"
                                                        + "    \"event\": {\n"
                                                        + "        \"id\": \"UPDATE\",\n"
                                                        + "        \"summary\": \"\",\n"
                                                        + "        \"description\": \"\"\n"
                                                        + "    },\n"
                                                        + "    \"event_token\": \"<event token>\",\n"
                                                        + "    \"ignore_warning\": false\n"
                                                        + "}"
                                                        + "\n```", required = true)
                                                    @RequestBody final CaseDataContent content) {
        if (!caseReferenceService.validateUID(caseId)) {
            throw new BadRequestException(V2.Error.CASE_ID_INVALID);
        }

        final CaseDetails caseDetails = createEventOperation.createCaseEvent(caseId, content);

        return status(HttpStatus.CREATED).body(new CaseResource(caseDetails, content));
    }

    @Transactional
    @PostMapping(
        path = "/case-types/{caseTypeId}/cases",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.CREATE_CASE
        }
    )
    @ApiOperation(
        value = "Submit case creation",
        notes = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponses({
        @ApiResponse(
            code = 201,
            message = "Created",
            response = CaseResource.class
        ),
        @ApiResponse(
            code = 400,
            message = V2.Error.MISSING_EVENT_TOKEN
        ),
        @ApiResponse(
            code = 404,
            message = V2.Error.EVENT_TRIGGER_NOT_FOUND
        ),
        @ApiResponse(
            code = 404,
            message = V2.Error.NO_MATCHING_EVENT_TRIGGER
        ),
        @ApiResponse(
            code = 409,
            message = V2.Error.CASE_ALTERED
        ),
        @ApiResponse(
            code = 422,
            message = V2.Error.CASE_DATA_NOT_FOUND
        ),
        @ApiResponse(
            code = 422,
            message = V2.Error.CASE_TYPE_NOT_FOUND
        ),
        @ApiResponse(
            code = 422,
            message = V2.Error.USER_ROLE_NOT_FOUND
        ),
        @ApiResponse(
            code = 422,
            message = V2.Error.EVENT_TRIGGER_NOT_SPECIFIED
        ),
        @ApiResponse(
            code = 422,
            message = V2.Error.EVENT_TRIGGER_NOT_KNOWN_FOR_CASE_TYPE
        ),
        @ApiResponse(
            code = 422,
            message = V2.Error.EVENT_TRIGGER_HAS_PRE_STATE
        ),
        @ApiResponse(
            code = 422,
            message = V2.Error.CASE_FIELD_INVALID
        ),
        @ApiResponse(
            code = 504,
            message = V2.Error.CALLBACK_EXCEPTION
        )
    })
    @LogAudit(operationType = CREATE_CASE, caseId = "#result.body.reference",
        jurisdiction = "#result.body.jurisdiction", caseType = "#caseTypeId", eventName = "#content.event.eventId")
    public ResponseEntity<CaseResource> createCase(@PathVariable("caseTypeId") String caseTypeId,
                                                   @RequestBody final CaseDataContent content,
                                                   @RequestParam(value = "ignore-warning", required = false)
                                                       final Boolean ignoreWarning) {
        final CaseDetails caseDetails = createCaseOperation.createCaseDetails(caseTypeId, content, ignoreWarning);

        return status(HttpStatus.CREATED).body(new CaseResource(caseDetails, content, ignoreWarning));
    }

    @Transactional
    @GetMapping(
        path = "/cases/{caseId}/events",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.CASE_EVENTS
        }
    )
    @ApiOperation(
        value = "Retrieve audit events by case ID",
        notes = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success",
            response = CaseEventsResource.class
        ),
        @ApiResponse(
            code = 400,
            message = V2.Error.ERROR_CASE_ID_INVALID
        ),
        @ApiResponse(
            code = 422,
            message = V2.Error.CASE_TYPE_DEF_NOT_FOUND_FOR_CASE_ID
        ),
        @ApiResponse(
            code = 422,
            message = V2.Error.ROLES_FOR_CASE_ID_NOT_FOUND
        ),
        @ApiResponse(
            code = 404,
            message = V2.Error.CASE_AUDIT_EVENTS_NOT_FOUND
        )
    })
    public ResponseEntity<CaseEventsResource> getCaseEvents(@PathVariable("caseId") String caseId) {
        if (!caseReferenceService.validateUID(caseId)) {
            throw new BadRequestException(V2.Error.ERROR_CASE_ID_INVALID);
        }

        final List<AuditEvent> auditEvents = getEventsOperation.getEvents(caseId);

        return ResponseEntity.ok(new CaseEventsResource(caseId, auditEvents));
    }


    @Transactional
    @PostMapping(
        path = "/cases/{caseId}/supplementary-data"
    )
    @ApiOperation(
        value = "Update Case Supplementary Data"
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Updated",
            response = SupplementaryDataResource.class
        ),
        @ApiResponse(
            code = 400,
            message = V2.Error.CASE_ID_INVALID
        ),
        @ApiResponse(
            code = 400,
            message = V2.Error.SUPPLEMENTARY_DATA_UPDATE_INVALID
        ),
        @ApiResponse(
            code = 400,
            message = V2.Error.MORE_THAN_ONE_NESTED_LEVEL
        ),
        @ApiResponse(
            code = 404,
            message = V2.Error.CASE_NOT_FOUND
        ),
        @ApiResponse(
            code = 403,
            message = V2.Error.NOT_AUTHORISED_UPDATE_SUPPLEMENTARY_DATA
        )
    })
    @ApiImplicitParams({
        @ApiImplicitParam(
            name = "supplementaryDataUpdateRequest",
            dataTypeClass = SupplementaryDataUpdateRequest.class,
            examples = @io.swagger.annotations.Example(
                value = {
                    @ExampleProperty(value = "{\n"
                        + "\t\"$inc\": {\n"
                        + "\t\t\"orgs_assigned_users.OrgA\": 1,\n"
                        + "\t\t\"orgs_assigned_users.OrgB\": -1\n"
                        + "\t},\n"
                        + "\t\"$set\": {\n"
                        + "\t\t\"orgs_assigned_users.OrgZ\": 34,\n"
                        + "\t\t\"processed\": true\n"
                        + "\t}\n"
                        + "}", mediaType = "application/json")
                }))
    })
    public ResponseEntity<SupplementaryDataResource> updateCaseSupplementaryData(@PathVariable("caseId") String caseId,
                                           @RequestBody SupplementaryDataUpdateRequest supplementaryDataUpdateRequest) {

        this.requestValidator.validate(supplementaryDataUpdateRequest);
        if (!caseReferenceService.validateUID(caseId)) {
            throw new BadRequestException(V2.Error.CASE_ID_INVALID);
        }
        SupplementaryData supplementaryDataUpdated = supplementaryDataUpdateOperation.updateSupplementaryData(caseId,
            supplementaryDataUpdateRequest);
        return status(HttpStatus.OK).body(new SupplementaryDataResource(supplementaryDataUpdated));
    }
}
