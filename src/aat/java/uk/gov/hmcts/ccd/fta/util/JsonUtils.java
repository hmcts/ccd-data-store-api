package uk.gov.hmcts.ccd.fta.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class JsonUtils {
    public static <T> T readObjectFromJsonResource(String resource, Class<T> objectType)
            throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        return mapper.readValue(inputStream, objectType);
    }

    public static <T> T readObjectFromJsonFile(String file, Class<T> objectType)
            throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(file), objectType);
    }

    public static <T> T readObjectFromJsonText(String jsonText, Class<T> objectType)
            throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonText, objectType);
    }

    public static String getJsonFromObject(Object object) throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(object);
    }

    public static String getPrettyJsonFromObject(Object object) throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    }
}
