package uk.gov.hmcts.ccd.datastore.befta.customvalue.handlers;

import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.ReflectionUtils;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueHandler;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueKey;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueKey.ORGANISATIONS_ASSIGNED_USERS;

public class OrganisationsAssignedUsersHandler implements CustomValueHandler {

    @Override
    public Boolean matches(CustomValueKey key) {
        return ORGANISATIONS_ASSIGNED_USERS.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        // extract args from key
        //    0 - path to context holding organisationIdentifier
        //    1 - (Optional) path to context holding previous value to use (otherwise: use 0)
        //    2 - (Optional) amount to increment previous value by (otherwise: don't increment)
        List<String> args = Arrays.asList(key.toString().replace("orgsAssignedUsers_","").split("\\|"));
        String organisationIdentifierContextPath = args.get(0);
        String previousValueContextPath = args.size() > 1 ? args.get(1) : null;
        int incrementBy = args.size() > 2 ? Integer.parseInt(args.get(2)) : 0;

        return calculateOrganisationsAssignedUsersPropertyWithValue(scenarioContext,
            organisationIdentifierContextPath,
            previousValueContextPath,
            incrementBy);
    }

    private Map<String, Object> calculateOrganisationsAssignedUsersPropertyWithValue(
        BackEndFunctionalTestScenarioContext scenarioContext,
        String organisationIdentifierContextPath,
        String previousValueContextPath,
        int incrementBy) {
        String organisationIdentifierFieldPath = organisationIdentifierContextPath
            + ".testData.actualResponse.body.organisationIdentifier";

        try {
            String organisationIdentifier = ReflectionUtils.deepGetFieldInObject(scenarioContext,
                organisationIdentifierFieldPath).toString();
            String propertyName = "orgs_assigned_users." + organisationIdentifier;

            int value = incrementBy; // default

            // if path to previous value supplied : read it
            if (previousValueContextPath != null) {
                String previousValueFieldPath = previousValueContextPath
                    + ".testData.actualResponse.body.supplementary_data."
                    + propertyName.replace(".", "\\.");
                Object previousValue = ReflectionUtils.deepGetFieldInObject(scenarioContext, previousValueFieldPath);
                if (previousValue != null) {
                    value = Integer.parseInt(previousValue.toString())  + incrementBy; // and increment
                }
            }
            return Collections.singletonMap(propertyName, value);
        } catch (Exception e) {
            throw new FunctionalTestException("Problem generating 'orgs_assigned_users' supplementary data property.",
                e);
        }
    }

}
