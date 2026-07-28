package datart.server.service.impl;

import datart.core.entity.Variable;
import datart.server.service.VariableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardServiceImplTest {

    private VariableService variableService;
    private DashboardServiceImpl dashboardService;
    private Variable orgVariable;

    @BeforeEach
    void setUp() {
        variableService = mock(VariableService.class);
        dashboardService = new DashboardServiceImpl(
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, variableService, null, null);
        orgVariable = variable("org-variable");
        when(variableService.listOrgQueryVariables("org-1"))
                .thenReturn(Collections.singletonList(orgVariable));
    }

    @Test
    void batchesViewVariablesAfterOrganizationVariables() {
        Set<String> viewIds = new LinkedHashSet<>(Arrays.asList("view-1", "view-2"));
        Variable viewVariable = variable("view-variable");
        when(variableService.listViewQueryVariablesByViewIds(viewIds))
                .thenReturn(Collections.singletonList(viewVariable));

        List<Variable> variables = dashboardService.collectQueryVariables("org-1", viewIds);

        assertEquals(Arrays.asList(orgVariable, viewVariable), variables);
        verify(variableService).listViewQueryVariablesByViewIds(viewIds);
        verify(variableService, never()).listViewQueryVariables("view-1");
        verify(variableService, never()).listViewQueryVariables("view-2");
    }

    @Test
    void skipsViewVariableLookupForEmptyViewIds() {
        List<Variable> variables = dashboardService.collectQueryVariables(
                "org-1", Collections.emptySet());

        assertEquals(Collections.singletonList(orgVariable), variables);
        verify(variableService, never()).listViewQueryVariablesByViewIds(Collections.emptySet());
    }

    private Variable variable(String id) {
        Variable variable = new Variable();
        variable.setId(id);
        return variable;
    }
}
