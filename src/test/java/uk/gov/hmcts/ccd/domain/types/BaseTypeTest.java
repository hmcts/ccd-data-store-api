package uk.gov.hmcts.ccd.domain.types;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("BaseType")
public class BaseTypeTest {
    private static final String TEXT_TYPE = "Text";

    @Mock
    private CaseDefinitionRepository definitionRepository;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        final FieldTypeDefinition documentFieldTypeDefinition = mock(FieldTypeDefinition.class);
        when(documentFieldTypeDefinition.getType()).thenReturn(TEXT_TYPE);

        when(definitionRepository.getBaseTypes()).thenReturn(Collections.emptyList());
        BaseType.setCaseDefinitionRepository(definitionRepository);
    }

    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    void initialiseBaseTypesWithMultiThreads() throws InterruptedException {
        FieldTypeDefinition documentFieldTypeDefinition = mock(FieldTypeDefinition.class);
        when(documentFieldTypeDefinition.getType()).thenReturn(TEXT_TYPE);
        when(definitionRepository.getBaseTypes()).thenReturn(Collections.singletonList(documentFieldTypeDefinition));

        List<BaseType> baseTypes = new ArrayList<>();
        Thread t1 = new Thread(() -> baseTypes.add(BaseType.get(TEXT_TYPE)));
        Thread t2 = new Thread(() -> baseTypes.add(BaseType.get(TEXT_TYPE)));
        Thread t3 = new Thread(() -> baseTypes.add(BaseType.get(TEXT_TYPE)));
        Thread t4 = new Thread(() -> baseTypes.add(BaseType.get(TEXT_TYPE)));
        Thread t5 = new Thread(() -> baseTypes.add(BaseType.get(TEXT_TYPE)));

        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t5.start();

        t1.join();
        t2.join();
        t3.join();
        t4.join();
        t5.join();

        assertEquals(1, baseTypes.stream().distinct().count());
    }

}
