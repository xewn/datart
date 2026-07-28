package datart.server.service.impl;

import datart.core.data.provider.SelectColumn;
import datart.core.entity.RelSubjectColumns;
import datart.core.entity.User;
import datart.core.entity.View;
import datart.core.mappers.ext.RelSubjectColumnsMapperExt;
import datart.security.manager.DatartSecurityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataProviderServiceImplTest {

    private RelSubjectColumnsMapperExt relationMapper;
    private DataProviderServiceImpl service;
    private View view;

    @BeforeEach
    void setUp() {
        relationMapper = mock(RelSubjectColumnsMapperExt.class);
        DatartSecurityManager securityManager = mock(DatartSecurityManager.class);
        User user = new User();
        user.setId("user-1");
        when(securityManager.getCurrentUser()).thenReturn(user);
        when(securityManager.isOrgOwner("org-1")).thenReturn(false);

        service = new DataProviderServiceImpl(null, relationMapper, null, null);
        service.setSecurityManager(securityManager);
        service.init();

        view = new View();
        view.setId("view-1");
        view.setOrgId("org-1");
    }

    @Test
    void grantsWildcardWhenNoColumnPermissionRelationsExist() {
        when(relationMapper.listByUser("view-1", "user-1")).thenReturn(Collections.emptyList());

        Set<SelectColumn> columns = parseColumnPermission();

        assertEquals(Collections.singleton("*"), columnKeys(columns));
    }

    @Test
    void deniesAllColumnsWhenExistingRelationsSelectNothing() {
        when(relationMapper.listByUser("view-1", "user-1")).thenReturn(Arrays.asList(
                relation("[]"),
                relation("[]")));

        Set<SelectColumn> columns = parseColumnPermission();

        assertTrue(columns.isEmpty());
    }

    @SuppressWarnings("unchecked")
    private Set<SelectColumn> parseColumnPermission() {
        return ReflectionTestUtils.invokeMethod(service, "parseColumnPermission", view);
    }

    private Set<String> columnKeys(Set<SelectColumn> columns) {
        return columns.stream().map(SelectColumn::getColumnKey).collect(Collectors.toSet());
    }

    private RelSubjectColumns relation(String permission) {
        RelSubjectColumns relation = new RelSubjectColumns();
        relation.setColumnPermission(permission);
        return relation;
    }
}
