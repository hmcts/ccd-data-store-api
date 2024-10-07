package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import java.io.Serializable;
import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.CustomType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.UserType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SupplementaryDataUserType implements UserType<JsonNode> {

    private final ObjectMapper mapper = new ObjectMapper();

    public static CustomType<JsonNode> CUSTOM_TYPE = new CustomType<JsonNode>(
        new SupplementaryDataUserType(), new TypeConfiguration()
    );

    @Override
    public int getSqlType() {
        return SqlTypes.JSON;
    }

    @Override
    public Class<JsonNode> returnedClass() {
        return JsonNode.class;
    }

    @Override
    public boolean equals(JsonNode x, JsonNode y) throws HibernateException {
        return x.equals(y);
    }

    @Override
    public int hashCode(JsonNode x) throws HibernateException {
        assert (x != null);
        return x.hashCode();
    }

    @Override
    public JsonNode nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner)
            throws SQLException {
        final String cellContent = rs.getString(position);
        if (cellContent == null) {
            return null;
        }
        try {
            return mapper.readValue(cellContent.getBytes("UTF-8"), returnedClass());
        } catch (final Exception ex) {
            throw new RuntimeException("Failed to convert String to MyJson: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement st, JsonNode value, int index, SharedSessionContractImplementor session)
            throws SQLException {
        if (value == null) {
            st.setNull(index, SqlTypes.JSON);
            return;
        }
        try {
            final StringWriter w = new StringWriter();
            mapper.writeValue(w, value);
            w.flush();
            st.setObject(index, w.toString(), SqlTypes.JSON);
        } catch (final Exception ex) {
            throw new RuntimeException("Failed to convert MyJson to String: " + ex.getMessage(), ex);
        }
    }

    @Override
    public JsonNode deepCopy(JsonNode value) {
        return value.deepCopy();
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(JsonNode value) {
        return (Serializable) value;
    }

    @Override
    public JsonNode assemble(Serializable cached, Object owner) {
        return (JsonNode) cached;
    }

    @Override
    public JsonNode replace(JsonNode detached, JsonNode managed, Object owner) {
        return detached;
    }

}