package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class SecurityClassificationServiceLogger {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityClassificationServiceLogger.class);

    private final SecurityClassificationServiceImpl securityClassificationService;

    final ObjectMapper objectMapper = new ObjectMapper();

    public SecurityClassificationServiceLogger(final SecurityClassificationServiceImpl securityClassificationService) {
        this.securityClassificationService = securityClassificationService;

        // Enables serialisation of java.util.Optional and java.time.LocalDateTime
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
    }

    private void jclog(String message) {
        LOG.info("JCDEBUG: SecurityClassificationServiceLogger: {}", message);
    }

    private void jclog(String message, Optional optional) {
        try {
            jclog(message + ": " + objectMapper.writeValueAsString(optional));
        } catch (JsonProcessingException e) {
            jclog(message + ": JSON ERROR");
        }
    }

    private void jclog(String message, JsonNode jsonNode) {
        try {
            jclog(message + ": " + objectMapper.writeValueAsString(jsonNode));
        } catch (JsonProcessingException e) {
            jclog(message + ": JSON ERROR");
        }
    }

    private void jclog(String message, CaseDetails caseDetails) {
        try {
            jclog(message + ": " + objectMapper.writeValueAsString(caseDetails));
        } catch (JsonProcessingException e) {
            jclog(message + ": JSON ERROR");
        }
    }

    private void jclog(String message, SecurityClassification securityClassification) {
        try {
            jclog(message + ": " + objectMapper.writeValueAsString(securityClassification));
        } catch (JsonProcessingException e) {
            jclog(message + ": JSON ERROR");
        }
    }

    private void jclog(String message, Boolean bool) {
        try {
            jclog(message + ": " + objectMapper.writeValueAsString(bool));
        } catch (JsonProcessingException e) {
            jclog(message + ": JSON ERROR");
        }
    }

    /*
     * TEST HARNESS :-
     * 1. Calls "MODIFIED" version of applyClassification() with logging (method below).
     * 2. Verifies hashcodes of "ORIGINAL" and "MODIFIED" filtered case details are the same.
     */
    public void applyClassification(final CaseDetails caseDetails,
                                    final Optional<CaseDetails> originalFilteredCaseDetails) {

        final Optional<CaseDetails> modifiedFilteredCaseDetails = applyClassificationModifiedVersion1(caseDetails,
                                                                                               false);
        try {
            final int originalHashCode = objectMapper.writeValueAsString(originalFilteredCaseDetails).hashCode();
            final int modifiedV1HashCode = objectMapper.writeValueAsString(modifiedFilteredCaseDetails).hashCode();
            jclog("originalHashCode and modifiedV1HashCode: "
                + (originalHashCode == modifiedV1HashCode ? "SAME" : "DIFFER"));
        } catch (JsonProcessingException e) {
            jclog("originalHashCode and modifiedHashCode: JSON ERROR: " + e.getMessage());
        }
    }

    /*
     * MODIFIED "version 1" applyClassification(CaseDetails caseDetails, boolean create).
     */
    public Optional<CaseDetails> applyClassificationModifiedVersion1(CaseDetails caseDetails, boolean create) {
        jclog("applyClassification (MODIFIED version 1)");
        Optional<SecurityClassification> userClassificationOpt =
            securityClassificationService.getUserClassification(caseDetails, create);
        jclog("    userClassificationOpt", userClassificationOpt);

        Function<SecurityClassification, Optional<CaseDetails>> flatmapFunctionV1 = new Function<SecurityClassification,
            Optional<CaseDetails>>() {
            @Override
            public Optional<CaseDetails> apply(SecurityClassification securityClassification) {
                Optional<CaseDetails> caseDetails1 = Optional.of(caseDetails);
                jclog("    caseDetails1", caseDetails1);
                Optional<CaseDetails> caseDetails2 = caseDetails1.filter(
                    caseHasClassificationEqualOrLowerThan(securityClassification));
                jclog("    caseDetails2", caseDetails2);
                Optional<CaseDetails> caseDetails3 = caseDetails2.map(
                    makeCaseDetailsMapFunction(caseDetails, securityClassification));
                jclog("    caseDetails3", caseDetails3);
                return caseDetails3;
            }
        };

        Optional<CaseDetails> caseDetails4 = userClassificationOpt.flatMap(flatmapFunctionV1);
        jclog("    caseDetails4", caseDetails4);
        return caseDetails4;
    }


    /*
     * Provides filter predicate for modified version of applyClassification().
     */
    private Predicate<CaseDetails> caseHasClassificationEqualOrLowerThan(SecurityClassification classification) {
        return new Predicate<CaseDetails>() {
            @Override
            public boolean test(CaseDetails cd) {
                jclog("    PREDICATE.CD", cd);
                final Function<SecurityClassification, Boolean> predicateMapFunction =
                                                                       new Function<SecurityClassification, Boolean>() {
                        @Override
                        public Boolean apply(SecurityClassification sc) {
                            jclog("    predicate.sc", sc);
                            SecurityClassification sc2 = cd.getSecurityClassification();
                            jclog("    PREDICATE.SC2", sc2);
                            Boolean result = sc.higherOrEqualTo(sc2);
                            jclog("    predicate.result", result);
                            return result;
                        }
                    };

                Optional<SecurityClassification> optionalSecurityClassification = Optional.ofNullable(classification);
                jclog("    predicate.optionalSecurityClassification", optionalSecurityClassification);
                Boolean result = optionalSecurityClassification.map(predicateMapFunction).orElse(false);
                jclog("    predicate.result", result);
                return result;
            }
        };
    }


    /*
     * Provides map function for modified version of applyClassification().
     */
    private Function<CaseDetails, CaseDetails> makeCaseDetailsMapFunction(final CaseDetails caseDetails,
                                                                  final SecurityClassification securityClassification) {
        return new Function<CaseDetails, CaseDetails>() {
            @Override
            public CaseDetails apply(CaseDetails cd) {
                if (cd.getDataClassification() == null) {
                    LOG.warn("No data classification for case with reference={}, all fields removed",
                        cd.getReference());
                    jclog("No data classification for case with reference="
                        + cd.getReference() + ", all fields removed");
                    cd.setDataClassification(Maps.newHashMap());
                }

                JsonNode data = securityClassificationService.filterNestedObject(
                    JacksonUtils.convertValueJsonNode(caseDetails.getData()),
                    JacksonUtils.convertValueJsonNode(cd.getDataClassification()),
                    securityClassification);
                jclog("    data", data);
                cd.setData(JacksonUtils.convertValue(data));
                jclog("    cd", cd);
                return cd;
            }
        };
    }


    // START OF INNER CLASS mapFunctionWrapper
    /*
    class MapFunctionWrapper {
        final CaseDetails caseDetails;
        final SecurityClassification securityClassification;

        MapFunctionWrapper(final CaseDetails caseDetails, final SecurityClassification securityClassification) {
            this.caseDetails = caseDetails;
            this.securityClassification = securityClassification;
        }

        Function<CaseDetails, CaseDetails> mapFunction = new Function<CaseDetails, CaseDetails>() {
            @Override
            public CaseDetails apply(CaseDetails cd) {
                if (cd.getDataClassification() == null) {
                    LOG.warn("No data classification for case with reference={}, all fields removed",
                        cd.getReference());
                    jclog("No data classification for case with reference="
                        + cd.getReference() + ", all fields removed");
                    cd.setDataClassification(Maps.newHashMap());
                }

                JsonNode data = securityClassificationService.filterNestedObject(
                    JacksonUtils.convertValueJsonNode(caseDetails.getData()),
                    JacksonUtils.convertValueJsonNode(cd.getDataClassification()),
                    securityClassification);
                jclog("    data", data);
                cd.setData(JacksonUtils.convertValue(data));
                jclog("    cd", cd);
                return cd;
            }
        };
    }
    */
    // END OF INNER CLASS mapFunctionWrapper

}
