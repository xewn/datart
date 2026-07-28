import {
  DateLevelType,
  DateLevelTypes,
  DateRatioType,
  DateRatioValueType,
} from 'app/constants';

export type DateRatioSetting = {
  column?: string[];
  snippet?: string;
  select?: string;
  ratioType: DateRatioType;
  valueType: DateRatioValueType;
};

export const dateLevel = (snippet?: string) => {
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

export const isDateRatioSettingValid = (
  hasDateDimension: boolean,
  setting: DateRatioSetting,
) =>
  hasDateDimension ||
  Boolean(
    setting.column?.length && dateLevel(setting.snippet) && setting.select,
  );
