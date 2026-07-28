import { DataViewFieldType } from 'app/constants';
import { canUseDateRatio } from '../AdvanceCalcAction';

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
