### Access Control w/ Role Assignment Service FTA Proof of Concept Quickstart

####Background & Concept
We will be writing a new set of tests to specifically test various access of access control within CCD specifically using the Role Assingment Service (no pseudo generation).
To do this we will be using the following confluence page as our 'Source of Truth', and the basis of from which we will build tests, decide what role assignments are needed and build a brand new definiton file from: https://tools.hmcts.net/confluence/display/RCCD/Access+Control+Worked+Examples

##### PreRequisites
- Make sure everything is configured locally to use the Role Assignment service, not the wiremock, the real deal
- Make sure you are using the `ccd-bypass` branch of role assignment service : `./ccd set am-role-assignment-service ccd-bypass`

##### New Definition File
From this confluence page we essentially have our test data to start. The tables representing tabs of a definition file have been used to build a new definition
file which can be found [here](https://github.com/hmcts/ccd-test-definitions/blob/RDM-12732/src/main/resources/uk/gov/hmcts/ccd/test_definitions/excel/Access_Control_Definition.xlsx), it is enough to get us started writing tests but subject to minor changes and tweaks, there are some field highlighted orange where additions
have been made that differs from the the conf page as the data from the confluence page isn't 100% valid in some scenarios. Now we have a new Test Definiton file to work with when writing new FTAs, there is also a `ccd-test-definitions` prerelease containing the def file already configured on this poc branch

Note: I think when I first uploaded this def file there was an error due to the ccd roles not existing in the db, you may be a manual step of adding the following new access profile roleNames to the user profile db via the ccd docker script bin/ccd-add-role.sh
- Solicitor
- Staff
- [APPELANT]       <- this is in square brackets
- [RESPONDENT]     <- this is in square brackets

In the conf page you may notice there is a J2 jurisdiction, this means there will be a second defintion file for the FTA's, it's not been created yet but it will soon

##### Users
We will be using new users for our new tests, for local development they will be added to the users.json file in ccd-docker so they can generated the normal way through the bin/add-users.sh

##### Role Assignments
Skip to the bottom of this section for a shortcut (it's in caps)
Our new users won't have any idam roles (just caseworker) they will need role assignments instead. As part of our test data we will need to set these up. There are two types, ORGANISATION RoleAssignments and CASE ra's. if we refer to the confluence
page we can see all the ones we will need for our FTA's, the ORGANISATIONAL ones are static, we always want them present in the db before starting out tests. for this there is some proof of concept setup in `java/uk/gov/hmcts/ccd/datastore/befta/RoleAssignmentSetup/RoleAssignmentSetup.java`
until we production this which is in progress now there are a few manual steps to get this working locally, there are some comments with instructions in the class but we need to do as follows:
- We have not generated the user and s2s token automatically. manually generate a user token for ccd.ac.solicitor1@gmail.com and a standard ccd_data s2s token
- in the same directory there are 2 json files, for role assignments for 2 users from the PoC tests. you will need to find the idam id for the following users and insert it into the `actorId` `reference` `assignerId` fields in the files:
- `ccd.ac.solicitor1@gmail.com` Idam id goes into `OrganisationRoleAssignmentsSolicitor1.json`
- `ccd.ac.superuser@gmail.com` Idam id goes into  `OrganisationRoleAssignmentsSuperUser.json`
  At this point best to run the `RoleAssignmentSetup` class to test the Role Assignments get successfully POSTed into the role assignment service - now we have our ORGANISATION role assingment data setup, also this class will get triggered at the same time as definition imports as it is called in the `DataStoreTestAutomationAdapter.java` Class.
  We will create any CASE ra's during our tests
    - NOTE: AFTER WRITING ALL THIS I REALISE THERE IS A MUCH EASIER WAY, I HAVE ADDED THE REQUIRED RA'S FOR THE POC FTA'S IN `https://github.com/hmcts/ccd-docker/blob/master/bin/am-role-assignments.json` so you can if you wish just use `./bin/add-role-assignments.sh` to just load the ra's straight into the db then disable line 56 in the `DataStoreTestAutomationAdapter` class to skip all the RA poc automated setup

##### Scenarios
This poc branch is pre configured to only run the 2 example scenarios for the new Access Control FTA's tagges `@ra`, they can be found in the following dirs:
- resources/features/F-200 - New Access Control Create Case
- resources/features/F-201 - New Access Control Get Case
  We are building our scenarios from the `Create Scenarios` and `Get Scenarios` tables in our source of truth confluence page referenced at the beginning of this doc, hopefully you can read across one of the table rows and cross-reference with the config of the def file and understand what the scenario is and why the result is what it is.
  Furthermore for the example scenarios I have written the tag format `S-200.N` and N is the row number from the table on the confluence page I have written the scenario from

##### reference links / paths
- Master confluence page: https://tools.hmcts.net/confluence/display/RCCD/Access+Control+Worked+Examples
- New Access Control Definition file:  https://github.com/hmcts/ccd-test-definitions/blob/RDM-12732/src/main/resources/uk/gov/hmcts/ccd/test_definitions/excel/Access_Control_Definition.xlsx
- New Users: https://github.com/hmcts/ccd-docker/blob/346af0ff5b05cd41a863dd7aa4aecdd9636df8ed/bin/users.json#L58
- Role Assignment data Setup Directory (poc): java/uk/gov/hmcts/ccd/datastore/befta/RoleAssignmentSetup
- VIDEO of demo of the poc: https://tools.hmcts.net/confluence/display/RCCD/CCD+Knowledge+Sharing+Sessions

