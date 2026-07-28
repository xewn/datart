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

import Config from '../config';
import PivotSheetChart, {
  buildSlashCornerShapes,
  createSlashCornerRenderer,
} from '../PivotSheetChart';

const headerTheme = {
  cell: { backgroundColor: '#f5f5f5' },
  text: { fill: '#202020', fontFamily: 'Arial', fontSize: 12 },
};

describe('<PivotSheetChart />', () => {
  let component;
  beforeEach(() => {
    component = new PivotSheetChart();
  });
  test('It should mount', () => {
    expect(component).toBeDatartChartModel();
  });

  test('exposes a disabled-by-default diagonal header style', () => {
    const styleGroup = Config.styles.find(style => style.key === 'style');
    const slashHeader = styleGroup.rows.find(row => row.key === 'enableSlash');

    expect(slashHeader).toEqual(
      expect.objectContaining({
        label: 'style.slashHeader',
        default: false,
        comType: 'checkbox',
      }),
    );
    expect(
      Config.i18ns.find(i18n => i18n.lang === 'zh-CN').translation.style
        .slashHeader,
    ).toBe('斜线表头');
    expect(
      Config.i18ns.find(i18n => i18n.lang === 'en-US').translation.style
        .slashHeader,
    ).toBe('Slash Header');
  });

  test('describes the diagonal corner without the S2 extra column', () => {
    expect(
      buildSlashCornerShapes({
        width: 180,
        height: 90,
        columns: ['Region', '$$extra$$', 'Year'],
        rows: ['Country', 'City'],
        theme: headerTheme,
      }),
    ).toMatchInlineSnapshot(`
      Array [
        Object {
          "attrs": Object {
            "fill": "#f5f5f5",
            "points": Array [
              Array [
                0,
                0,
              ],
              Array [
                0,
                90,
              ],
              Array [
                180,
                90,
              ],
              Array [
                180,
                0,
              ],
            ],
          },
          "type": "polygon",
        },
        Object {
          "attrs": Object {
            "lineWidth": 1,
            "stroke": "#202020",
            "x1": 0,
            "x2": 180,
            "y1": 0,
            "y2": 90,
          },
          "type": "line",
        },
        Object {
          "attrs": Object {
            "fill": "#202020",
            "fontFamily": "Arial",
            "fontSize": 12,
            "text": "Region/Year",
            "x": 72,
            "y": 30,
          },
          "type": "text",
          "zIndex": 100,
        },
        Object {
          "attrs": Object {
            "fill": "#202020",
            "fontFamily": "Arial",
            "fontSize": 12,
            "text": "Country/City",
            "x": 20,
            "y": 86.4,
          },
          "type": "text",
          "zIndex": 100,
        },
      ]
    `);
  });

  test('injects the custom renderer only when the option is enabled', () => {
    expect(createSlashCornerRenderer(false, headerTheme)).toBeUndefined();

    const node = {
      cfg: { width: 180, height: 90 },
      addShape: jest.fn(),
    };
    const renderer = createSlashCornerRenderer(true, headerTheme);
    renderer(
      node,
      { theme: { cornerCell: {} } },
      {
        data: [
          { cornerType: 'col', value: 'Region' },
          { cornerType: 'col', value: '$$extra$$' },
          { cornerType: 'row', value: 'Country' },
        ],
      },
    );

    expect(node.addShape.mock.calls).toEqual(
      buildSlashCornerShapes({
        width: 180,
        height: 90,
        columns: ['Region', '$$extra$$'],
        rows: ['Country'],
        theme: headerTheme,
      }).map(({ type, ...shape }) => [type, shape]),
    );
  });
});
