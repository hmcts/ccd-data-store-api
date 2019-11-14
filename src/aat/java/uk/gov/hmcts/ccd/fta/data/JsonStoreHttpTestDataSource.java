package uk.gov.hmcts.ccd.fta.data;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import uk.gov.hmcts.jsonstore.JsonResourceStoreWithInheritance;

import java.io.IOException;
import java.util.ArrayList;

public class JsonStoreHttpTestDataSource implements HttpTestDataSource {

    private ArrayList<String> resourcePaths = new ArrayList<>();

    private JsonResourceStoreWithInheritance jsonStore;

    public JsonStoreHttpTestDataSource(String[] resourcePackages) {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(
                this.getClass().getClassLoader());
        for (String resourcePackage : resourcePackages) {
            try {
                String packagePath = this.getClass().getClassLoader().getResource(resourcePackage).getPath();
                Resource[] resources = resolver
                        .getResources("classpath*:" + resourcePackage + "/**/*.json");
                for (Resource resource : resources)
                {
                    String resourcePath = resource.getURL().getPath();
                    resourcePaths.add(resourcePackage + resourcePath.substring(packagePath.length()));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public HttpTestData getDataForScenario(String scenarioKey) {
        loadDataStoreIfNotAlreadyLoaded();
        try {
            return jsonStore.getObjectWithId(scenarioKey, HttpTestData.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loadDataStoreIfNotAlreadyLoaded() {
        jsonStore = new JsonResourceStoreWithInheritance(resourcePaths.toArray(new String[0]));
    }

}
