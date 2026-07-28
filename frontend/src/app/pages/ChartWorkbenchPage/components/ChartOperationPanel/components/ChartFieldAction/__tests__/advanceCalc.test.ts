import { DataViewFieldType } from 'app/constants';
import { canUseDateRatio } from '../AdvanceCalcAction';
import { isDateRatioSettingValid } from '../dateRatio';

describe('advanced calculation availability', () => {
  test('requires both a date field and a supported date-level function', () => {
    expect(canUseDateRatio([], [])).toBe(false);
    expect(
      canUseDateRatio(
        [{ type: DataViewFieldType.DATE } as any],
        ['AGG_DATE_MONTH'],
      ),
    ).toBe(true);
    expect(
      canUseDateRatio(
        [{ type: DataViewFieldType.STRING } as any],
        ['AGG_DATE_MONTH'],
      ),
    ).toBe(false);
  });
});

describe('date ratio configuration', () => {
  const base = {
    column: ['created_at'],
    snippet: 'AGG_DATE_MONTH(created_at)',
    ratioType: 'last' as any,
    valueType: 'percent' as any,
  };

  test('requires a selected period when there is no grouped date dimension', () => {
    expect(isDateRatioSettingValid(false, base)).toBe(false);
    expect(isDateRatioSettingValid(false, { ...base, select: '2024-02' })).toBe(
      true,
    );
  });

  test('does not require a selected period for grouped date dimensions', () => {
    expect(isDateRatioSettingValid(true, base)).toBe(true);
  });
});
