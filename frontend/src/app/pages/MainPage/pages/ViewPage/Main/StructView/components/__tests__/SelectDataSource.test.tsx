import { render, screen, waitFor } from '@testing-library/react';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { useSearchAndExpand } from 'app/hooks/useSearchAndExpand';
import { selectSources } from 'app/pages/MainPage/pages/SourcePage/slice/selectors';
import { selectAllSourceDatabaseSchemas } from 'app/pages/MainPage/pages/ViewPage/slice/selectors';
import { useDispatch, useSelector } from 'react-redux';
import { ThemeProvider } from 'styled-components';
import { getTableAllColumns } from '../../../../utils';
import SelectDataSource from '../SelectDataSource';

jest.mock('react-redux', () => ({
  useDispatch: jest.fn(),
  useSelector: jest.fn(),
}));
jest.mock('app/components', () => {
  const React = require('react');
  return {
    MenuListItem: ({ children }) => React.createElement('div', null, children),
    Tree: () => null,
  };
});
jest.mock('app/hooks/useI18NPrefix');
jest.mock('app/hooks/useSearchAndExpand');
jest.mock('app/pages/MainPage/pages/SourcePage/slice/selectors', () => ({
  selectSources: jest.fn(),
}));
jest.mock('app/pages/MainPage/pages/ViewPage/slice/selectors', () => ({
  selectAllSourceDatabaseSchemas: jest.fn(),
}));
jest.mock('app/pages/MainPage/pages/ViewPage/slice/thunks', () => ({
  getSchemaBySourceId: jest.fn(),
}));
jest.mock('../../../../utils', () => ({
  buildAntdTreeNodeModel: jest.fn(),
  getTableAllColumns: jest.fn(),
}));

const mockUseDispatch = useDispatch as jest.Mock;
const mockUseSelector = useSelector as jest.Mock;
const mockUseI18NPrefix = useI18NPrefix as jest.Mock;
const mockUseSearchAndExpand = useSearchAndExpand as jest.Mock;
const mockGetTableAllColumns = getTableAllColumns as jest.Mock;
const theme = { primary: '#1677ff' } as any;
const sources = [{ id: 'source-1', name: 'Source 1' }];
const schemas = { 'source-1': [] };

const renderSelectDataSource = (joinTable?: any) => (
  <ThemeProvider theme={theme}>
    <SelectDataSource
      type="JOINS"
      sourceId="source-1"
      joinTable={joinTable}
      allowManage
    />
  </ThemeProvider>
);

describe('SelectDataSource', () => {
  beforeEach(() => {
    mockUseDispatch.mockReturnValue(jest.fn());
    mockUseSelector.mockImplementation(selector =>
      selector === selectSources
        ? sources
        : selector === selectAllSourceDatabaseSchemas
        ? schemas
        : undefined,
    );
    mockUseI18NPrefix.mockReturnValue((key: string) => key);
    mockUseSearchAndExpand.mockReturnValue({
      filteredData: [],
      debouncedSearch: jest.fn(),
    });
    mockGetTableAllColumns.mockReturnValue([]);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  test('clears the selected table when a join table is removed', async () => {
    const { rerender } = render(
      renderSelectDataSource({ table: ['orders'], columns: ['id'] }),
    );

    await screen.findByText('orders');

    rerender(renderSelectDataSource());

    await waitFor(() => expect(screen.getByText('selectTable')).toBeVisible());
  });

  test('shows a replacement join table', async () => {
    const { rerender } = render(
      renderSelectDataSource({ table: ['orders'], columns: ['id'] }),
    );

    await screen.findByText('orders');

    rerender(
      renderSelectDataSource({ table: ['customers'], columns: ['id'] }),
    );

    await screen.findByText('customers');
  });
});
