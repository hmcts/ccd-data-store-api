package uk.gov.hmcts.ccd.migration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import pl.jalokim.propertiestojson.util.PropertiesToJsonConverter;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

public class SupplementaryDataQueryGenerator {

    private static final String CCD_FILE_TYPE = "CCD";
    private static final String RAS_FILE_TYPE = "RAS";
    private static final String UO_FILE_TYPE = "UO";
    private static final String SEPARATOR = ",";
    private static final String ORG_SEPARATOR = "\\|";
    private static final String CASE_ORG_SEPARATOR = "--";
    private static final String INPUT_FOLDER = "src/main/resources/migration/input/";
    private static String OUTPUT_FILE = "src/main/resources/migration/output/";
    private static final String PARENT_KEY = "orgs_assigned_users";
    private static final Integer FIELD_VALUE = 1;

    private static final List<RoleAssignment> roleAssignmentList = new ArrayList<>();
    private static final List<UserOrganisation> userOrganisationList = new ArrayList<>();
    private static final List<CaseOrganisation> caseOrganisationList = new ArrayList<>();
    private static final List<CaseData> caseDataList = new ArrayList<>();
    private static final Set<String> caseReferenceSet = new HashSet<>();
    private static final Map<String, Integer> caseOrgMap = new HashMap<>();

    //json_value, parent_path, value, parent_key, json_value_insert, node_path, leaf_node_key, value, node_path, leaf_node_key, node_path, value, reference
    private static final String UPDATE_QUERY = "UPDATE case_data SET "
        + "supplementary_data= (CASE"
        + "        WHEN COALESCE(supplementary_data, '{}') = '{}' "
        + "        THEN COALESCE(supplementary_data, '{}') || '%s'::jsonb"
        + "        WHEN jsonb_extract_path_text(COALESCE(supplementary_data, '{}'), '%s') IS NULL AND %d > 0"
        + "        THEN jsonb_insert(COALESCE(supplementary_data, '{}'), '%s', '%s'::jsonb)"
        + "        WHEN jsonb_extract_path_text(COALESCE(supplementary_data, '{}'), '%s') IS NULL "
        + "        THEN jsonb_set(COALESCE(supplementary_data, '{}'), '%s', %d::TEXT::jsonb)"
        + "        WHEN jsonb_extract_path_text(COALESCE(supplementary_data, '{}'), '%s') IS NOT NULL "
        + "        THEN jsonb_set(COALESCE(supplementary_data, '{}'), '%s',"
        + "             (jsonb_extract_path_text(supplementary_data, '%s')::INT + %d) ::TEXT::jsonb, false)"
        + "    END) "
        + "WHERE reference = %s;";

    public static void main (String[] args) {

        try {
            //Asylum
            loadFile(CCD_FILE_TYPE, INPUT_FOLDER + "CCD-Asylum.txt");
            loadFile(RAS_FILE_TYPE, INPUT_FOLDER + "RAS-Asylum.txt");
            loadFile(UO_FILE_TYPE, INPUT_FOLDER + "RD-Asylum.txt");
            OUTPUT_FILE = OUTPUT_FILE + "Asylum-Queries.txt";

            //CIVIL 1
            /*loadFile(CCD_FILE_TYPE, INPUT_FOLDER + "CCD-CIVIL-PART-1.txt");
            loadFile(RAS_FILE_TYPE, INPUT_FOLDER + "RAS-CIVIL-PART-1.txt");
            loadFile(UO_FILE_TYPE, INPUT_FOLDER + "RD-CIVIL-PART-1.txt");
            OUTPUT_FILE = OUTPUT_FILE + "CIVIL-PART-1-Queries.txt";*/

            //CIVIL 2
            /*loadFile(CCD_FILE_TYPE, INPUT_FOLDER + "CCD-CIVIL-PART-2.txt");
            loadFile(RAS_FILE_TYPE, INPUT_FOLDER + "RAS-CIVIL-PART-2.txt");
            loadFile(UO_FILE_TYPE, INPUT_FOLDER + "RD-CIVIL-PART-2.txt");
            OUTPUT_FILE = OUTPUT_FILE + "CIVIL-PART-2-Queries.txt";*/

            //FRMVP2
            /*loadFile(CCD_FILE_TYPE, INPUT_FOLDER + "CCD-FRMVP2.txt");
            loadFile(RAS_FILE_TYPE, INPUT_FOLDER + "RAS-FRMVP2.txt");
            loadFile(UO_FILE_TYPE, INPUT_FOLDER + "RD-FRMVP2.txt");
            OUTPUT_FILE = OUTPUT_FILE + "FRMVP2-Queries.txt";*/

            //FRContested
            /*loadFile(CCD_FILE_TYPE, INPUT_FOLDER + "CCD-FRContested.txt");
            loadFile(RAS_FILE_TYPE, INPUT_FOLDER + "RAS-FRContested.txt");
            loadFile(UO_FILE_TYPE, INPUT_FOLDER + "RD-FRContested.txt");
            OUTPUT_FILE = OUTPUT_FILE + "FRContested-Queries.txt";*/

            generateCaseOrganisation();
            generateOutput();
            generateUniqueCaseOrgReport();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void loadFile(String fileType, String filePath) throws IOException {
        Path path = Paths.get(filePath);
        BufferedReader reader = Files.newBufferedReader(path);
        String line;

        if ((RAS_FILE_TYPE.compareTo(fileType)) == 0) {
            while ((line = reader.readLine()) != null) {
                loadRoleAssignments(line);
            }
        } else if ((UO_FILE_TYPE.compareTo(fileType)) == 0) {
            while ((line = reader.readLine()) != null) {
                loadUserOrganisation(line);
            }
        } else {
            while ((line = reader.readLine()) != null) {
                loadCaseDetails(line);
            }
        }
    }

    private static void loadCaseDetails(String line) {
        String[] caseData = line.split(SEPARATOR);

        if (caseData.length == 5) {
        String[] orgArray = caseData[4].split(ORG_SEPARATOR);
            List<String> orgList = new ArrayList<>();

            for (String s : orgArray) {
                if (!"".equals(s.trim())) {
                    orgList.add(s);
                }
            }

            if (!orgList.isEmpty()) {
                caseDataList.add(new CaseData(caseData[0], orgList));
            }
        }
    }

    private static void loadRoleAssignments(String line) {
        String[] roleAssignmentData = line.split(SEPARATOR);
        roleAssignmentList.add(new RoleAssignment(roleAssignmentData[0], roleAssignmentData[1], roleAssignmentData[2]));
    }

    private static void loadUserOrganisation(String line) {
        String[] userOrganisationData = line.split(SEPARATOR);
        userOrganisationList.add(new UserOrganisation(userOrganisationData[0], userOrganisationData[1]));
    }

    private static void generateCaseOrganisation() {
        caseDataList.forEach(caseData -> roleAssignmentList.forEach(ra -> {
            if (caseData.getCaseReference().equals(ra.getCaseReference())) {
                userOrganisationList.forEach(uo -> {
                    if (caseData.getOrganisationId().contains(uo.getOrganisationId()) &&
                        ra.getUserId().equals(uo.getUserId())) {
                        caseOrganisationList.add(new CaseOrganisation(ra.getCaseReference(), uo.getOrganisationId()));

                        caseReferenceSet.add(caseData.getCaseReference());

                        String key = caseData.getCaseReference() + "|" + uo.getOrganisationId();
                        if (caseOrgMap.containsKey(key)) {
                            Integer value = caseOrgMap.get(key);
                            caseOrgMap.put(key, value + 1);
                        } else {
                            caseOrgMap.put(key, 1);
                        }
                    }
                });
            }
        }));
    }

    //json_value, parent_path/parent_key, value, parent_key, json_value_insert, node_path, leaf_node_key, value, node_path, leaf_node_key, node_path, value, reference
    private static void generateOutput() throws IOException {
        try (FileWriter fileWriter = new FileWriter(OUTPUT_FILE)) {
            caseOrganisationList.forEach(co -> {
                String fieldPath = PARENT_KEY + "." + co.getOrganisationId();
                String leafNodeKey = fieldPath.replaceAll(Pattern.quote("."), ",");
                String leafNodeKeyStr = "{" + leafNodeKey + "}";
                String jsonValue = requestedDataToJson(fieldPath, FIELD_VALUE);
                String jsonValueInsert = requestedDataJsonForPath(fieldPath, FIELD_VALUE, PARENT_KEY);
                List<String> nodePathList = Arrays.asList(fieldPath.split(Pattern.quote(".")));
                String nodePathStr = "";
                for (String str : nodePathList) {
                    nodePathStr += "'" + str + "',";
                }
                String nodePathStrFinal = nodePathStr.substring(1, nodePathStr.length()-2);
                String parentKeyStr = "{" + PARENT_KEY + "}";
                List<String> parentPathList = Arrays.asList(PARENT_KEY);
                String parentPathStr = parentPathList.toString().substring(1,parentPathList.toString().length()-1);

                String line = "\n" + String.format(UPDATE_QUERY, jsonValue, parentPathStr, FIELD_VALUE, parentKeyStr,
                    jsonValueInsert, nodePathStrFinal, leafNodeKeyStr, FIELD_VALUE, nodePathStrFinal, leafNodeKeyStr,
                    nodePathStrFinal, FIELD_VALUE, co.getCaseReference()) + "\n";
                try {
                    fileWriter.write(line);
                    fileWriter.flush();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
        }
    }

    private static void generateUniqueCaseOrgReport() throws IOException {
        System.out.println("Number of actual cases validated: " + caseDataList.size());
        System.out.println("Number of unique cases will be updated: " + caseReferenceSet.size());
        System.out.println("Number of cases (with different org) will be updated: " + caseOrgMap.size());
        System.out.println("Number of queries will be executed: " + caseOrganisationList.size());

        System.out.println("Below cases will be updated");
        caseReferenceSet.forEach(System.out::println);

        System.out.println("Below unique cases (with org) will be updated");
        caseOrgMap.forEach((k, v) -> System.out.println(k + CASE_ORG_SEPARATOR + v));
    }

    private static String requestedDataToJson(String fieldPath, Object fieldValue) {
        PropertiesToJsonConverter propertiesMapper = new PropertiesToJsonConverter();
        Properties properties = new Properties();
        properties.put(fieldPath, fieldValue);
        return propertiesMapper.convertToJson(properties);
    }

    private static String requestedDataJsonForPath(String fieldPath, Object fieldValue, String pathToMatch) {
        String jsonString = requestedDataToJson(fieldPath, fieldValue);
        DocumentContext context = JsonPath.parse(jsonString);

        try {
            Object value = context.read("$." + pathToMatch, Object.class);
            return jsonNodeToString(value);
        } catch (PathNotFoundException e) {
            throw new ServiceException(String.format("Path %s is not found", pathToMatch));
        }
    }

    private static String jsonNodeToString(Object data) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new ServiceException("Unable to map object to JSON string", e);
        }
    }
}
