import { render, waitFor } from '@testing-library/react';
import { ChartLifecycle } from 'app/constants';
import ChartIFrameEventBroker from '../ChartIFrameEventBroker';
import ChartIFrameLifecycleAdapter from '../ChartIFrameLifecycleAdapter';
import ChartIFrameResourceLoader from '../ChartIFrameResourceLoader';

const mockLoadResource = jest.fn().mockResolvedValue(undefined);
const mockPublish = jest.fn();
const mockRegister = jest.fn();
const mockDispose = jest.fn();

jest.mock('app/components/ReactFrameComponent', () => ({
  useFrame: () => ({ document: global.document, window: global.window }),
}));
jest.mock('app/hooks/useI18NPrefix', () => () => (key: string) => key);
jest.mock('../ChartIFrameResourceLoader', () => jest.fn());
jest.mock('../ChartIFrameEventBroker', () => jest.fn());
jest.mock('uuid/dist/umd/uuidv4.min');

const MockChartIFrameResourceLoader = ChartIFrameResourceLoader as jest.Mock;
const MockChartIFrameEventBroker = ChartIFrameEventBroker as jest.Mock;

const chart = {
  meta: { id: 'chart-1' },
  getDependencies: jest.fn(() => []),
  init: jest.fn(),
} as any;
const config = {} as any;
const dataset = [{ value: 1 }];

const countLifecycle = (lifecycle: ChartLifecycle) =>
  mockPublish.mock.calls.filter(([event]) => event === lifecycle).length;

describe('ChartIFrameLifecycleAdapter', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockLoadResource.mockResolvedValue(undefined);
    MockChartIFrameResourceLoader.mockImplementation(() => ({
      loadResource: mockLoadResource,
    }));
    MockChartIFrameEventBroker.mockImplementation(() => ({
      publish: mockPublish,
      register: mockRegister,
      dispose: mockDispose,
    }));
  });

  test('does not resize for loading-only changes', async () => {
    const { rerender } = render(
      <ChartIFrameLifecycleAdapter
        chart={chart}
        config={config}
        dataset={dataset}
        style={{ width: 100, height: 80 }}
        isLoadingData={false}
      />,
    );

    await waitFor(() =>
      expect(countLifecycle(ChartLifecycle.Mounted)).toBe(1),
    );
    await waitFor(() =>
      expect(countLifecycle(ChartLifecycle.Resize)).toBeGreaterThan(0),
    );
    const initialResizeCount = countLifecycle(ChartLifecycle.Resize);

    rerender(
      <ChartIFrameLifecycleAdapter
        chart={chart}
        config={config}
        dataset={dataset}
        style={{ width: 100, height: 80 }}
        isLoadingData
      />,
    );
    rerender(
      <ChartIFrameLifecycleAdapter
        chart={chart}
        config={config}
        dataset={dataset}
        style={{ width: 100, height: 80 }}
        isLoadingData={false}
      />,
    );

    expect(countLifecycle(ChartLifecycle.Resize)).toBe(initialResizeCount);

    rerender(
      <ChartIFrameLifecycleAdapter
        chart={chart}
        config={config}
        dataset={dataset}
        style={{ width: 120, height: 80 }}
        isLoadingData={false}
      />,
    );

    expect(countLifecycle(ChartLifecycle.Resize)).toBe(initialResizeCount + 1);
  });
});
