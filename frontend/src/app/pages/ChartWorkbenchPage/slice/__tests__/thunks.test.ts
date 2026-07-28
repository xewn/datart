import {
  ChartDataViewFieldCategory,
  DataViewFieldType,
} from 'app/constants';
import { convertToChartDto } from 'app/utils/ChartDtoHelper';
import { request2 } from 'utils/request';
import { fetchChartAction } from '../thunks';

jest.mock('react-monaco-editor', () => ({ monaco: {} }));
jest.mock('utils/request', () => ({ request2: jest.fn() }));
jest.mock('app/utils/ChartDtoHelper', () => ({
  buildCreateChartRequest: jest.fn(),
  buildUpdateChartRequest: jest.fn(),
  convertToChartDto: jest.fn(data => ({
    ...data,
    config: JSON.parse(data.config),
  })),
}));
jest.mock('..', () => ({
  __esModule: true,
  default: { actions: {} },
  initState: {},
}));

const mockRequest2 = request2 as jest.Mock;
const mockConvertToChartDto = convertToChartDto as jest.Mock;
const runFetchChart = (arg: any) =>
  fetchChartAction(arg)(jest.fn(), jest.fn(() => ({})), undefined);

const backendChart = {
  id: 'chart-1',
  name: 'Chart',
  orgId: 'org-1',
  status: 1,
  viewId: 'view-1',
  view: {
    meta: [
      {
        name: 'created_at',
        path: ['created_at'],
        type: DataViewFieldType.DATE,
      },
    ],
  },
  config: {
    chartGraphId: 'table',
    computedFields: [
      {
        name: 'custom_field',
        expression: '1 + 1',
        type: DataViewFieldType.NUMERIC,
        category: ChartDataViewFieldCategory.ComputedField,
      },
    ],
  },
};

describe('fetchChartAction', () => {
  beforeEach(() => {
    mockConvertToChartDto.mockImplementation(data => ({
      ...data,
      config: JSON.parse(data.config),
    }));
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  test('restores date-level computed fields for an embedded chart', async () => {
    const action: any = await runFetchChart({ backendChart });
    const computedFields = action.payload.config.computedFields;

    expect(
      computedFields.some(
        field =>
          field.category ===
          ChartDataViewFieldCategory.DateLevelComputedField,
      ),
    ).toBe(true);
    expect(computedFields).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ name: 'custom_field' }),
      ]),
    );
    expect(action.payload).toEqual(
      expect.objectContaining({ id: 'chart-1', name: 'Chart' }),
    );
  });

  test('returns an embedded chart without config unchanged', async () => {
    const chartWithoutConfig = { ...backendChart, config: undefined };

    const action: any = await runFetchChart({
      backendChart: chartWithoutConfig,
    });

    expect(action.payload).toBe(chartWithoutConfig);
  });

  test('retains the existing API chart conversion path', async () => {
    mockRequest2.mockResolvedValue({
      data: {
        ...backendChart,
        config: JSON.stringify(backendChart.config),
      },
    });

    const action: any = await runFetchChart({ chartId: 'chart-1' });

    expect(mockRequest2).toHaveBeenCalledWith({
      method: 'GET',
      url: 'viz/datacharts/chart-1',
    });
    expect(action.payload.config).toEqual(backendChart.config);
  });
});
