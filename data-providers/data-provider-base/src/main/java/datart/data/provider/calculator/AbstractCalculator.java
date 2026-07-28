package datart.data.provider.calculator;

import datart.core.data.provider.DataProvider;
import datart.core.data.provider.DataProviderSource;
import datart.core.data.provider.Dataframe;
import datart.core.data.provider.ExecuteParam;
import datart.core.data.provider.QueryScript;
import datart.core.data.provider.sql.AggregateOperator;

public abstract class AbstractCalculator {

    public abstract String type();

    public abstract void calculate(Dataframe dataframe,
                                   AggregateOperator aggregate,
                                   ExecuteParam executeParam,
                                   DataProviderSource source,
                                   QueryScript queryScript,
                                   DataProvider dataProvider) throws Exception;
}
