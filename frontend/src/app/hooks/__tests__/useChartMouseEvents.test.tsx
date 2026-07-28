import { render } from '@testing-library/react';
import { useChartMouseEvents } from '../useChartMouseEvents';

const Harness = ({ chart, events }) => {
  useChartMouseEvents(chart, events);
  return null;
};

const createChart = () => ({ registerMouseEvents: jest.fn() });

describe('useChartMouseEvents', () => {
  test('replaces and clears mouse events with the chart lifecycle', () => {
    const chartA = createChart();
    const chartB = createChart();
    const eventsA = [{ name: 'click', callback: jest.fn() }];
    const eventsB = [{ name: 'click', callback: jest.fn() }];
    const { rerender, unmount } = render(
      <Harness chart={chartA} events={eventsA} />,
    );

    expect(chartA.registerMouseEvents).toHaveBeenCalledTimes(1);
    expect(chartA.registerMouseEvents).toHaveBeenLastCalledWith(eventsA);

    rerender(<Harness chart={chartA} events={eventsA} />);
    expect(chartA.registerMouseEvents).toHaveBeenCalledTimes(1);

    rerender(<Harness chart={chartB} events={eventsA} />);
    expect(chartA.registerMouseEvents).toHaveBeenLastCalledWith([]);
    expect(chartB.registerMouseEvents).toHaveBeenCalledTimes(1);
    expect(chartB.registerMouseEvents).toHaveBeenLastCalledWith(eventsA);

    rerender(<Harness chart={chartB} events={eventsB} />);
    expect(chartB.registerMouseEvents).toHaveBeenNthCalledWith(2, []);
    expect(chartB.registerMouseEvents).toHaveBeenNthCalledWith(3, eventsB);

    unmount();
    expect(chartB.registerMouseEvents).toHaveBeenNthCalledWith(4, []);
  });
});
