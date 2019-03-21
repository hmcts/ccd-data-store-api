package uk.gov.hmcts.ccd.data;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.gov.hmcts.ccd.data.IdListCacheKeyGenerator;
import uk.gov.hmcts.ccd.data.definition.DefaultCaseDefinitionRepository;

public class IdListCacheKeyGeneratorTest {

	private IdListCacheKeyGenerator underTest = new IdListCacheKeyGenerator();
	
	DefaultCaseDefinitionRepository repository = new DefaultCaseDefinitionRepository(null, null, null);
	Method method;
	
	
	@Before
	public void setup() throws Exception{
        method = repository.getClass().getMethod("getJurisdictions", List.class);
	}

	@Test
    public void testKeyGenerationForNullOrEmptyList() {

		Assert.assertEquals("DefaultCaseDefinitionRepository_getJurisdictions_", underTest.generate(repository, method, new Object[] { null }));

		Assert.assertEquals("DefaultCaseDefinitionRepository_getJurisdictions_", underTest.generate(repository, method, new Object[] { new ArrayList<>() }));
    }

	@Test
    public void testKeyGenerationForListsOfDifferentOrderings() {
		Assert.assertEquals("DefaultCaseDefinitionRepository_getJurisdictions_J1", underTest.generate(repository, method, new Object[] { Arrays.asList("J1") }));

		Assert.assertEquals("DefaultCaseDefinitionRepository_getJurisdictions_J1,J2", underTest.generate(repository, method, new Object[] { Arrays.asList("J1", "J2") }));

		Assert.assertEquals("DefaultCaseDefinitionRepository_getJurisdictions_J1,J2,J3", underTest.generate(repository, method, new Object[] { Arrays.asList("J2", "J3", "J1") }));
    }

}
