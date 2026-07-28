/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.data.provider.jdbc.adapters;

import datart.core.data.provider.Column;
import datart.core.data.provider.Dataframe;
import datart.core.data.provider.ExecuteParam;
import datart.core.data.provider.ForeignKey;
import datart.core.data.provider.QueryScript;
import datart.core.data.provider.sql.AggregateOperator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class BigQueryDataProviderAdapter extends JdbcDataProviderAdapter {

    private static final Pattern VALID_FIELD_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,299}");

    @Override
    public Set<String> readAllDatabases() throws SQLException {
        Set<String> databases = new HashSet<>();
        try (Connection connection = getConn();
             ResultSet schemas = connection.getMetaData().getSchemas()) {
            while (schemas.next()) {
                databases.add(schemas.getString(1));
            }
        }
        return databases;
    }

    @Override
    public Set<String> readAllTables(String database) throws SQLException {
        Set<String> tables = new HashSet<>();
        try (Connection connection = getConn()) {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet resultSet = metadata.getTables(
                    connection.getCatalog(), database, "%", new String[]{"TABLE", "VIEW"})) {
                while (resultSet.next()) {
                    tables.add(resultSet.getString(3));
                }
            }
        }
        return tables;
    }

    @Override
    public Set<Column> readTableColumn(String database, String table) throws SQLException {
        Set<Column> columns = new HashSet<>();
        try (Connection connection = getConn()) {
            DatabaseMetaData metadata = connection.getMetaData();
            Map<String, List<ForeignKey>> importedKeys = getImportedKeys(metadata, database, table);
            try (ResultSet resultSet = metadata.getColumns(
                    connection.getCatalog(), database, table, "%")) {
                while (resultSet.next()) {
                    Column column = readTableColumn(resultSet);
                    column.setForeignKeys(importedKeys.get(column.columnKey()));
                    columns.add(column);
                }
            }
        }
        return columns;
    }

    @Override
    public Dataframe executeOnSource(QueryScript script, ExecuteParam executeParam) throws Exception {
        Map<String, String> aliases = useSafeAggregateAliases(executeParam);
        Dataframe dataframe = null;
        try {
            dataframe = super.executeOnSource(script, executeParam);
            return dataframe;
        } finally {
            restoreAggregateAliases(executeParam, aliases, dataframe);
        }
    }

    static Map<String, String> useSafeAggregateAliases(ExecuteParam executeParam) {
        Map<String, String> aliases = new LinkedHashMap<>();
        if (executeParam == null || CollectionUtils.isEmpty(executeParam.getAggregators())) {
            return aliases;
        }
        int index = 0;
        for (AggregateOperator aggregator : executeParam.getAggregators()) {
            String alias = aggregator.getAlias();
            if (StringUtils.isNotBlank(alias) && !VALID_FIELD_NAME.matcher(alias).matches()) {
                String safeAlias = "DATART_AGG_" + index++;
                aliases.put(safeAlias, alias);
                aggregator.setAlias(safeAlias);
            }
        }
        return aliases;
    }

    static void restoreAggregateAliases(ExecuteParam executeParam,
                                        Map<String, String> aliases,
                                        Dataframe dataframe) {
        if (aliases.isEmpty()) {
            return;
        }
        if (executeParam != null && !CollectionUtils.isEmpty(executeParam.getAggregators())) {
            for (AggregateOperator aggregator : executeParam.getAggregators()) {
                String originalAlias = aliases.get(aggregator.getAlias());
                if (originalAlias != null) {
                    aggregator.setAlias(originalAlias);
                }
            }
        }
        if (dataframe != null && !CollectionUtils.isEmpty(dataframe.getColumns())) {
            for (Column column : dataframe.getColumns()) {
                String originalAlias = aliases.get(column.columnName());
                if (originalAlias != null) {
                    column.setName(originalAlias);
                }
            }
        }
    }
}
