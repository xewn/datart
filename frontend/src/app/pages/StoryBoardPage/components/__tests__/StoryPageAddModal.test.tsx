import { act, render, screen } from '@testing-library/react';
import React from 'react';
import StoryPageAddModal from '../StoryPageAddModal';

let mockModalProps: any;
let mockTableProps: any;

jest.mock('antd', () => ({
  Modal: props => {
    const React = require('react');
    mockModalProps = props;
    return React.createElement(
      'section',
      null,
      React.createElement('h1', null, props.title),
      props.children,
    );
  },
  Table: props => {
    const React = require('react');
    mockTableProps = props;
    return React.createElement('div', { 'data-testid': 'story-page-table' });
  },
}));

jest.mock('i18next', () => ({
  t: (key: string) => key,
}));

const pageContents = [
  { relId: 'existing-dashboard', name: 'Existing dashboard' },
  { relId: 'new-dashboard', name: 'New dashboard' },
] as any;

const sortedPages = [
  {
    id: 'story-page-1',
    relId: 'existing-dashboard',
    storyboardId: 'story-1',
    relType: 'DASHBOARD',
    config: { index: 0 },
  },
] as any;

const ModalUnderTest = StoryPageAddModal as React.ComponentType<any>;

describe('StoryPageAddModal', () => {
  test('keeps existing pages selected and submits only newly selected pages', () => {
    const onSelectedPages = jest.fn();
    const props = {
      pageContents,
      sortedPages,
      onSelectedPages,
      onCancel: jest.fn(),
    };
    const { rerender } = render(<ModalUnderTest {...props} visible={false} />);

    rerender(<ModalUnderTest {...props} visible />);

    expect(
      screen.getByRole('heading', {
        name: 'viz.board.setting.addStoryPage',
      }),
    ).toBeInTheDocument();
    expect(mockTableProps.rowSelection.selectedRowKeys).toEqual([
      'existing-dashboard',
    ]);
    expect(
      mockTableProps.rowSelection.getCheckboxProps(pageContents[0]),
    ).toEqual({ disabled: true });
    expect(
      mockTableProps.rowSelection.getCheckboxProps(pageContents[1]),
    ).toEqual({ disabled: false });

    act(() => {
      mockTableProps.rowSelection.onChange([
        'existing-dashboard',
        'new-dashboard',
      ]);
    });
    act(() => {
      mockModalProps.onOk();
    });

    expect(onSelectedPages).toHaveBeenCalledWith(['new-dashboard']);
  });
});
