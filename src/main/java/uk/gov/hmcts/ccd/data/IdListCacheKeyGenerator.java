package uk.gov.hmcts.ccd.data;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.tomcat.util.buf.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

@Component
@Qualifier("idListCacheKeyGenerator")
public class IdListCacheKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        @SuppressWarnings("unchecked")
        List<String> idList = params[0] ==  null ? new ArrayList<>() : (List<String>) params[0];
        Collections.sort(idList);
        String idsCombined = StringUtils.join(idList);
        return target.getClass().getSimpleName() + "_" + method.getName() + "_" + idsCombined;
    }

}
