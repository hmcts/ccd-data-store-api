package uk.gov.hmcts.jsonstore;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ReflectionUtils {

    public static Object deepGetFieldInObject(Object object, String fieldPath) throws Exception {
        if (object == null)
            return null;
        if (fieldPath == null || fieldPath.length() == 0)
            throw new IllegalArgumentException("Field path must be non-empty String.");
        String[] fields = fieldPath.split("\\.");
        Object fieldValue = retrieveFieldInObject(object, fields[0]);
        for (int i = 1; i < fields.length; i++) {
            fieldValue = retrieveFieldInObject(fieldValue, fields[i]);
        }
        return fieldValue;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Object retrieveFieldInObject(Object object, String fieldName) throws Exception {
        if (object == null)
            return null;
        if (fieldName == null || fieldName.length() == 0)
            throw new IllegalArgumentException("fieldName must be non-empty String.");

        System.out.println("Will retrieve [" + fieldName + "] from [" + object + "] of type " + object.getClass());

        String getterName = "get" + StringUtils.firstLetterToUpperCase(fieldName);

        Throwable thrown = null;
        try {
            Method method = object.getClass().getMethod(getterName);
            return method.invoke(object);
        } catch (Throwable t) {
            thrown = t;
        }

        if (object instanceof Map)
            return ((Map) object).get(fieldName);
        else if (object instanceof Iterable) {
            Collection values = null;
            if (object instanceof Set)
                values = new LinkedHashSet<>();
            else
                values = new ArrayList<>();

            Iterator itr = ((Iterable) object).iterator();
            while (itr.hasNext()) {
                Object elementInside = retrieveFieldInObject(itr.next(), fieldName);
                if (elementInside == null)
                    elementInside = new DummyPlaceHolder();
                values.add(elementInside);
            }

            if (values.size() == 0)
                return null;
            else if (values.size() == 1)
                return values.iterator().next();
            return values;
        }
        if (thrown != null)
            thrown.printStackTrace();
        throw new NoSuchFieldException(fieldName + " not retrievable from " + object + ".");
    }

    public static class DummyPlaceHolder {
    }

    @SuppressWarnings({ "rawtypes" })
    public static boolean mapContains(Map<?, ?> big, Map<?, ?> small) {
        if (big == null)
            return small == null;
        else if (small == null)
            throw new IllegalArgumentException("Small cannot be null when big is not.");

        if (small.size() > big.size())
            return false;
        for (Object key : small.keySet()) {
            Object smallValue = small.get(key);
            Object bigValue = big.get(key);
            if (smallValue instanceof Map) {
                boolean valueContained = mapContains((Map) bigValue, (Map) smallValue);
                if (!valueContained)
                    return false;
            } else if (small != null) {
                boolean valueEquals = smallValue.equals(bigValue);
                if (!valueEquals)
                    return false;
            }
        }
        return true;
    }

}
