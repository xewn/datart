import { ChartMouseEvent, IChart } from 'app/types/Chart';
import { useEffect } from 'react';

export const useChartMouseEvents = (
  chart: IChart | undefined,
  events: ChartMouseEvent[],
) => {
  useEffect(() => {
    chart?.registerMouseEvents(events);
    return () => chart?.registerMouseEvents([]);
  }, [chart, events]);
};
