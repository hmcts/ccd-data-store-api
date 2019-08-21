package uk.gov.hmcts.ccd.data.helper;

import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;

public class AccessManagementQueryHelper {
    private DataSource dataSource;

    public AccessManagementQueryHelper(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Integer findExplicitAccessPermissions(String jurisdiction) {
        String query = ("select COUNT (*) from access_management where service_name = ?");
        return new JdbcTemplate(dataSource).queryForObject(query, Integer.class, jurisdiction);
    }

    public void deleteAllFromAccessManagementTables() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String query = ("delete from access_management;" +
                        "delete from roles;" +
                        "delete from resources;" +
                        "delete from resource_attributes;" +
                        "delete from default_permissions_for_roles;" +
                        "delete from services;");
        jdbcTemplate.update(query);
    }
}
