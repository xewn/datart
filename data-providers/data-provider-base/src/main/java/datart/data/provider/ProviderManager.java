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

package datart.data.provider;

import com.alibaba.fastjson.JSON;
import datart.core.base.exception.Exceptions;
import datart.core.base.processor.ExtendProcessor;
import datart.core.base.processor.ProcessorResponse;
import datart.core.data.provider.*;
import datart.core.data.provider.processor.DataProviderPostProcessor;
import datart.core.data.provider.processor.DataProviderPreProcessor;
import datart.core.data.provider.sql.AggregateOperator;
import datart.data.provider.calculator.CalculatorFactory;
import datart.data.provider.optimize.DataProviderExecuteOptimizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ProviderManager extends DataProviderExecuteOptimizer implements DataProviderManager {

    private static final Pattern AGGREGATE_COLUMN_PATTERN = Pattern.compile(
            "^\\s*[A-Za-z_][A-Za-z0-9_]*\\s*\\(\\s*(?:DISTINCT\\s+)?([^()]+?)\\s*\\)\\s*$",
            Pattern.CASE_INSENSITIVE);

    @Autowired(required = false)
    private List<ExtendProcessor> extendProcessors = new ArrayList<ExtendProcessor>();

    private static final Map<String, DataProvider> cachedDataProviders = new ConcurrentHashMap<>();

    private static final CalculatorFactory CALCULATORS = CalculatorFactory.load();

    public Map<String, DataProvider> getDataProviders() {
        if (cachedDataProviders.isEmpty()) {
            synchronized (ProviderManager.class) {
                if (cachedDataProviders.isEmpty()) {
                    ServiceLoader<DataProvider> load = ServiceLoader.load(DataProvider.class);
                    for (DataProvider dataProvider : load) {
                        try {
                            cachedDataProviders.put(dataProvider.getType(), dataProvider);
                        } catch (IOException e) {
                            log.error("", e);
                        }
                    }
                }
            }
        }
        return cachedDataProviders;
    }

    @Override
    public List<DataProviderInfo> getSupportedDataProviders() {
        ArrayList<DataProviderInfo> providerInfos = new ArrayList<>();
        for (DataProvider dataProvider : getDataProviders().values()) {
            try {
                providerInfos.add(dataProvider.getBaseInfo());
            } catch (IOException e) {
                log.error("DataProvider init error {" + dataProvider.getClass().getName() + "}", e);
            }
        }
        return providerInfos;
    }

    @Override
    public DataProviderConfigTemplate getSourceConfigTemplate(String type) throws IOException {
        DataProvider providerService = getDataProviderService(type);
        DataProviderConfigTemplate configTemplate = providerService.getConfigTemplate();
        if (!CollectionUtils.isEmpty(configTemplate.getAttributes())) {
            for (DataProviderConfigTemplate.Attribute attribute : configTemplate.getAttributes()) {
                attribute.setDisplayName(providerService.getConfigDisplayName(attribute.getName()));
                attribute.setDescription(providerService.getConfigDescription(attribute.getName()));
                if (!CollectionUtils.isEmpty(attribute.getChildren())) {
                    for (DataProviderConfigTemplate.Attribute child : attribute.getChildren()) {
                        child.setDisplayName(providerService.getConfigDisplayName(child.getName()));
                        child.setDescription(providerService.getConfigDescription(child.getName()));
                    }
                }
            }
        }
        return configTemplate;
    }

    @Override
    public Object testConnection(DataProviderSource source) throws Exception {
        return getDataProviderService(source.getType()).test(source);
    }

    @Override
    public Set<String> readAllDatabases(DataProviderSource source) throws SQLException {
        return getDataProviderService(source.getType()).readAllDatabases(source);
    }

    @Override
    public Set<String> readTables(DataProviderSource source, String database) throws SQLException {
        return getDataProviderService(source.getType()).readTables(source, database);
    }

    @Override
    public Set<Column> readTableColumns(DataProviderSource source, String database, String table) throws SQLException {
        return getDataProviderService(source.getType()).readTableColumns(source, database, table);
    }

    @Override
    public Dataframe execute(DataProviderSource source, QueryScript queryScript, ExecuteParam param) throws Exception {

        //sql + param preprocessing
        ProcessorResponse preProcessorRes = this.preProcessorQuery(source, queryScript, param);
        if (!preProcessorRes.isSuccess()) {
            return Dataframe.empty();
        }
        Dataframe dataframe;

        DataProvider dataProvider = getDataProviderService(source.getType());

        String queryKey = dataProvider.getQueryKey(source, queryScript, param) + calculationKey(param);

        if (param.isCacheEnable()) {
            dataframe = getFromCache(queryKey);
            if (dataframe != null) {
                return dataframe;
            }
        }
        if (param.isConcurrencyOptimize()) {
            dataframe = runOptimize(queryKey, source, queryScript, param);
        } else {
            dataframe = run(source, queryScript, param);
        }
        if (param.isCacheEnable()) {
            setCache(queryKey, dataframe, param.getCacheExpires());
        }
        //data postprocessing
        ProcessorResponse postProcessorRes = this.postProcessorQuery(dataframe, source, queryScript, param);
        if (!postProcessorRes.isSuccess()) {
            return Dataframe.empty();
        }

        return dataframe;

    }

    private ProcessorResponse preProcessorQuery(DataProviderSource source, QueryScript queryScript, ExecuteParam param) {
        if (!CollectionUtils.isEmpty(extendProcessors)) {
            for (ExtendProcessor processor : extendProcessors) {
                if (processor instanceof DataProviderPreProcessor) {
                    ProcessorResponse response = ((DataProviderPreProcessor) processor).preRun(source, queryScript, param);
                    if (!response.isSuccess()) {
                        return response;
                    }
                }
            }
        }
        return ProcessorResponse.success();
    }

    private ProcessorResponse postProcessorQuery(Dataframe dataframe, DataProviderSource source, QueryScript queryScript, ExecuteParam param) {
        if (!CollectionUtils.isEmpty(extendProcessors)) {
            for (ExtendProcessor processor : extendProcessors) {
                if (processor instanceof DataProviderPostProcessor) {
                    ProcessorResponse response = ((DataProviderPostProcessor) processor).postRun(dataframe, source, queryScript, param);
                    if (!response.isSuccess()) {
                        return response;
                    }
                }
            }
        }
        return ProcessorResponse.success();
    }

    @Override
    public Set<StdSqlOperator> supportedStdFunctions(DataProviderSource source) {
        return getDataProviderService(source.getType()).supportedStdFunctions(source);
    }

    @Override
    public boolean validateFunction(DataProviderSource source, String snippet) {
        DataProvider provider = getDataProviderService(source.getType());
        return provider.validateFunction(source, snippet);
    }

    @Override
    public void updateSource(DataProviderSource source) {
        DataProvider providerService = getDataProviderService(source.getType());
        providerService.resetSource(source);
    }

    void excludeColumns(Dataframe data, Set<SelectColumn> include) {
        if (data == null
                || CollectionUtils.isEmpty(data.getColumns())
                || include == null
                || include.stream().anyMatch(selectColumn ->
                    selectColumn != null && "*".equals(selectColumn.getColumnKey()))) {
            return;
        }

        List<Integer> excludeIndex = new LinkedList<>();
        for (int i = 0; i < data.getColumns().size(); i++) {
            Column column = data.getColumns().get(i);
            if (include
                    .stream()
                    .noneMatch(selectColumn -> includesColumn(column.columnKey(), selectColumn))) {
                excludeIndex.add(i);
            }
        }
        if (excludeIndex.size() > 0 && !CollectionUtils.isEmpty(data.getRows())) {
            data.getRows().parallelStream().forEach(row -> {
                for (Integer index : excludeIndex) {
                    row.set(index, null);
                }
            });
        }
    }

    static String calculationKey(ExecuteParam param) {
        if (param == null || CollectionUtils.isEmpty(param.getAggregators())) {
            return ";calculations:[]";
        }
        return ";calculations:" + JSON.toJSONString(canonicalize(JSON.toJSON(param.getAggregators())));
    }

    private static Object canonicalize(Object value) {
        if (value instanceof Map) {
            Map<String, Object> sorted = new TreeMap<>();
            ((Map<?, ?>) value).forEach((key, item) ->
                    sorted.put(String.valueOf(key), canonicalize(item)));
            return sorted;
        }
        if (value instanceof Collection) {
            List<Object> items = new ArrayList<>();
            for (Object item : (Collection<?>) value) {
                items.add(canonicalize(item));
            }
            return items;
        }
        return value;
    }

    private boolean includesColumn(String columnKey, SelectColumn selectedColumn) {
        if (selectedColumn == null) {
            return false;
        }
        if (columnKey.equals(selectedColumn.getColumnKey())
                || columnKey.equals(selectedColumn.getAlias())) {
            return true;
        }
        Matcher matcher = AGGREGATE_COLUMN_PATTERN.matcher(columnKey);
        return matcher.matches() && matcher.group(1).trim().equals(selectedColumn.getColumnKey());
    }


    private DataProvider getDataProviderService(String type) {
        DataProvider dataProvider = getDataProviders().get(type);
        if (dataProvider == null) {
            Exceptions.msg("No data provider type " + type);
        }
        return dataProvider;
    }


    @Override
    public Dataframe run(DataProviderSource source, QueryScript queryScript, ExecuteParam param) throws Exception {
        DataProvider dataProvider = getDataProviderService(source.getType());
        Dataframe dataframe = dataProvider.execute(source, queryScript, param);
        applyCalculations(dataframe, source, queryScript, param, dataProvider);
        excludeColumns(dataframe, param.getIncludeColumns());
        return dataframe;
    }

    private void applyCalculations(Dataframe dataframe,
                                   DataProviderSource source,
                                   QueryScript queryScript,
                                   ExecuteParam param,
                                   DataProvider dataProvider) throws Exception {
        if (param.getAggregators() == null) {
            return;
        }
        for (AggregateOperator aggregate : param.getAggregators()) {
            if (aggregate != null && aggregate.getCalc() != null) {
                CALCULATORS.create(aggregate.getCalc().getType())
                        .calculate(dataframe, aggregate, param, source, queryScript, dataProvider);
            }
        }
    }

}
