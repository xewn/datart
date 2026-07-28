/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { DatePicker, Radio, Select, Space } from 'antd';
import { FormItemEx } from 'app/components';
import {
  AdvanceCalcFieldActionType,
  ChartDataSectionType,
  DateLevelType,
  DateLevelTypes,
  DateRatioType,
  DateRatioValueType,
} from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import ChartDataViewContext from 'app/pages/ChartWorkbenchPage/contexts/ChartDataViewContext';
import ChartPaletteContext from 'app/pages/ChartWorkbenchPage/contexts/ChartPaletteContext';
import { ChartDataSectionField } from 'app/types/ChartConfig';
import { updateBy } from 'app/utils/mutation';
import cryptoRandomString from 'crypto-random-string';
import moment, { Moment } from 'moment';
import {
  FC,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import styled from 'styled-components/macro';
import { getAllFieldsOfEachType } from '../../utils';

type DateRatioSetting = {
  column?: string[];
  snippet?: string;
  select?: string;
  ratioType: DateRatioType;
  valueType: DateRatioValueType;
};

const dateLevel = (snippet?: string) => {
  const operator = DateLevelTypes.find(type => snippet?.startsWith(type));
  switch (operator) {
    case DateLevelType.AggDateYear:
      return { picker: 'year' as const, format: 'YYYY' };
    case DateLevelType.AggDateQuarter:
      return { picker: 'quarter' as const, format: 'YYYY-Q' };
    case DateLevelType.AggDateMonth:
      return { picker: 'month' as const, format: 'YYYY-MM' };
    case DateLevelType.AggDateWeek:
      return { picker: 'week' as const, format: 'GGGG-WW' };
    case DateLevelType.AggDateDay:
      return { picker: 'date' as const, format: 'YYYY-MM-DD' };
    default:
      return undefined;
  }
};

const DateRatioAction: FC<{
  config: ChartDataSectionField;
  onConfigChange: (
    config: ChartDataSectionField,
    needRefresh?: boolean,
  ) => void;
}> = ({ config, onConfigChange }) => {
  const t = useI18NPrefix('viz.palette.data.dateRatio');
  const { dataView, availableSourceFunctions } =
    useContext(ChartDataViewContext);
  const { datas } = useContext(ChartPaletteContext);
  const dateFields = useMemo(() => {
    const { dateLevelFields } = getAllFieldsOfEachType({
      sortType: 'byNameSort',
      dataView,
      availableSourceFunctions,
    });
    return dateLevelFields.filter(field => field.children?.length);
  }, [availableSourceFunctions, dataView]);
  const hasDateDimension = Boolean(
    datas?.some(
      section =>
        [ChartDataSectionType.Group, ChartDataSectionType.Mixed].includes(
          section.type as ChartDataSectionType,
        ) &&
        section.rows?.some(row =>
          DateLevelTypes.some(level => row.expression?.startsWith(level)),
        ),
    ),
  );
  const initial = useMemo<DateRatioSetting>(() => {
    const existing = config.calc?.config || {};
    const firstField = dateFields[0];
    return {
      column: existing.column || firstField?.path,
      snippet: existing.snippet || firstField?.children?.[0]?.expression,
      select: existing.select,
      ratioType: existing.ratioType || DateRatioType.Last,
      valueType: existing.valueType || DateRatioValueType.Percent,
    };
  }, [config.calc?.config, dateFields]);
  const [setting, setSetting] = useState(initial);
  const calcKey = useRef(config.calc?.key || cryptoRandomString(8));

  const buildFieldConfig = useCallback(
    (next: DateRatioSetting) =>
      updateBy(config, draft => {
        draft.calc = {
          key: calcKey.current,
          type: AdvanceCalcFieldActionType.Ratio,
          config: hasDateDimension
            ? {
                ratioType: next.ratioType,
                valueType: next.valueType,
              }
            : next,
        };
      }),
    [config, hasDateDimension],
  );

  useEffect(() => {
    onConfigChange(buildFieldConfig(initial), true);
  }, [buildFieldConfig, initial, onConfigChange]);

  const updateSetting = (patch: Partial<DateRatioSetting>) => {
    const next = { ...setting, ...patch };
    setSetting(next);
    onConfigChange(buildFieldConfig(next), true);
  };

  const selectedFieldIndex = Math.max(
    0,
    dateFields.findIndex(field =>
      Boolean(
        field.path?.length === setting.column?.length &&
          field.path?.every((part, index) => part === setting.column?.[index]),
      ),
    ),
  );
  const selectedField = dateFields[selectedFieldIndex];
  const picker = dateLevel(setting.snippet);
  const selectedMoment =
    setting.select && picker
      ? moment(setting.select, picker.format, true)
      : undefined;
  const pickerValue: Moment | undefined = selectedMoment?.isValid()
    ? selectedMoment
    : undefined;

  return (
    <StyledDateRatioAction direction="vertical">
      {!hasDateDimension && selectedField && (
        <>
          <FormItemEx label={t('field')}>
            <Select
              value={selectedFieldIndex}
              onChange={(index: number) => {
                const field = dateFields[index];
                updateSetting({
                  column: field.path,
                  snippet: field.children?.[0]?.expression,
                  select: undefined,
                });
              }}
            >
              {dateFields.map((field, index) => (
                <Select.Option key={field.name} value={index}>
                  {field.name}
                </Select.Option>
              ))}
            </Select>
          </FormItemEx>
          <FormItemEx label={t('dimension')}>
            <Radio.Group
              value={setting.snippet}
              onChange={event =>
                updateSetting({
                  snippet: event.target.value,
                  select: undefined,
                })
              }
            >
              {selectedField.children?.map(field => (
                <Radio key={field.expression} value={field.expression}>
                  {field.name}
                </Radio>
              ))}
            </Radio.Group>
          </FormItemEx>
          <FormItemEx label={t('select')}>
            <DatePicker
              value={pickerValue}
              format={picker?.format}
              picker={picker?.picker}
              disabled={!picker}
              onChange={(_, value) =>
                updateSetting({ select: value || undefined })
              }
            />
          </FormItemEx>
        </>
      )}
      <FormItemEx label={t('ratioType')}>
        <Radio.Group
          value={setting.ratioType}
          onChange={event => updateSetting({ ratioType: event.target.value })}
        >
          <Radio value={DateRatioType.Last}>{t('ratioLast')}</Radio>
          <Radio value={DateRatioType.Year}>{t('ratioYear')}</Radio>
        </Radio.Group>
      </FormItemEx>
      <FormItemEx label={t('valueType')}>
        <Radio.Group
          value={setting.valueType}
          onChange={event => updateSetting({ valueType: event.target.value })}
        >
          <Radio value={DateRatioValueType.Percent}>{t('percent')}</Radio>
          <Radio value={DateRatioValueType.Diff}>{t('diff')}</Radio>
        </Radio.Group>
      </FormItemEx>
    </StyledDateRatioAction>
  );
};

export default DateRatioAction;

const StyledDateRatioAction = styled(Space)`
  width: 100%;
`;
