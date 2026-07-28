/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import BasicTableChart from '../BasicTableChart';

describe('<BasicTableChart />', () => {
  let component;
  beforeEach(() => {
    component = new BasicTableChart();
  });
  test('It should mount', () => {
    expect(component).toBeDatartChartModel();
  });

  const createDataSet = rows =>
    Object.assign(rows, {
      getFieldKey: jest.fn(() => 'field-key'),
      getFieldIndex: jest.fn(() => 0),
      getFieldOriginKey: jest.fn(() => 'origin-field'),
    });
  const dataConfig = {
    uid: 'field-uid',
    colName: 'field-name',
    alias: { name: 'Field' },
  };

  test('returns an empty cell config for a missing row', () => {
    const columns = component.getFlatColumns(
      [dataConfig],
      createDataSet([]),
      [],
    );

    expect(columns[0].onCell({}, 0)).toEqual({});
  });

  test('retains normal cell config for a present row', () => {
    const row = {
      getCell: jest.fn(() => 42),
      getFieldKey: jest.fn(() => 'row-field-key'),
    };
    const columns = component.getFlatColumns(
      [dataConfig],
      createDataSet([row]),
      [],
    );

    expect(columns[0].onCell({}, 0)).toEqual(
      expect.objectContaining({
        uid: 'field-uid',
        cellValue: 42,
        dataIndex: 'row-field-key',
        sensitiveFieldName: 'origin-field',
        rowData: row,
        rowIndex: 0,
        onClick: expect.any(Function),
      }),
    );
  });
});
