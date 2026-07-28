import { ControllerFacadeTypes, TimeFilterValueCategory } from 'app/constants';
import { TIME_FORMATTER } from 'globalConstants';
import { formatTime } from 'app/utils/time';
import { applyControllerUrlValue } from '../widget';

const createDateContent = (type: ControllerFacadeTypes) =>
  ({
    type,
    config: {
      controllerDate: {
        pickerType: 'date',
        startTime: {
          relativeOrExact: TimeFilterValueCategory.Exact,
          exactValue: 'old-start',
        },
        endTime: {
          relativeOrExact: TimeFilterValueCategory.Exact,
          exactValue: 'old-end',
        },
      },
    },
  } as any);

describe('applyControllerUrlValue', () => {
  test('uses both URL values for a time range', () => {
    const content = createDateContent(ControllerFacadeTypes.RangeTime);

    applyControllerUrlValue(content, [
      '2024-01-01 00:00:00',
      '2024-01-31 23:59:59',
    ]);

    expect(content.config.controllerDate.startTime.exactValue).toBe(
      '2024-01-01 00:00:00',
    );
    expect(content.config.controllerDate.endTime.exactValue).toBe(
      '2024-01-31 23:59:59',
    );
  });

  test('falls back to the start value for a one-value time range', () => {
    const content = createDateContent(ControllerFacadeTypes.RangeTime);

    applyControllerUrlValue(content, ['2024-02-29 12:00:00']);

    expect(content.config.controllerDate.startTime.exactValue).toBe(
      '2024-02-29 12:00:00',
    );
    expect(content.config.controllerDate.endTime.exactValue).toBe(
      '2024-02-29 12:00:00',
    );
  });

  test('formats the first URL value for a single-time controller', () => {
    const content = createDateContent(ControllerFacadeTypes.Time);
    const value = '2024-03-04 05:06:07';

    applyControllerUrlValue(content, [value, 'ignored']);

    expect(content.config.controllerDate.startTime).toEqual({
      relativeOrExact: TimeFilterValueCategory.Exact,
      exactValue: formatTime(value, TIME_FORMATTER),
    });
  });

  test('keeps non-date controller URL assignment unchanged', () => {
    const content = {
      type: ControllerFacadeTypes.DropdownList,
      config: { controllerValues: ['old'] },
    } as any;

    applyControllerUrlValue(content, ['new']);

    expect(content.config.controllerValues).toEqual(['new']);
  });
});
