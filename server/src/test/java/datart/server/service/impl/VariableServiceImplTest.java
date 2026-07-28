package datart.server.service.impl;

import datart.core.entity.Variable;
import datart.core.mappers.ext.VariableMapperExt;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class VariableServiceImplTest {

    @Test
    void skipsMapperForEmptyViewIds() {
        VariableMapperExt variableMapper = mock(VariableMapperExt.class);
        VariableServiceImpl service = new VariableServiceImpl(variableMapper, null, null);

        List<Variable> variables = service.listViewQueryVariablesByViewIds(Collections.emptySet());

        assertTrue(variables.isEmpty());
        verifyNoInteractions(variableMapper);
    }
}
