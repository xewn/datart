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
import { CUSTOM_BTN_DEFAULT, FONT_DEFAULT } from 'app/constants';
import { ORIGINAL_TYPE_MAP } from 'app/pages/DashBoardPage/constants';
import type {
  WidgetActionListItem,
  widgetActionType,
  WidgetMeta,
  WidgetProto,
  WidgetToolkit,
} from 'app/pages/DashBoardPage/types/widgetTypes';
import { getJsonConfigs } from 'app/pages/DashBoardPage/utils';
import { WHITE } from 'styles/StyleConstants';
import { ICustomBtnDefault, IFontDefault } from '../../../../../../types';
import {
  initBackgroundTpl,
  initBorderTpl,
  initInteractionTpl,
  initPaddingTpl,
  initWidgetName,
  InteractionI18N,
  PaddingI18N,
  TitleI18N,
  widgetTpl,
} from '../../WidgetManager/utils/init';

const initCustomBtnTpl = () => {
  return [
    {
      label: 'customBtn.meta',
      key: 'metaGroup',
      comType: 'group',
      rows: [
        {
          label: 'customBtn.btnFormat',
          key: 'btnFormat',
          comType: 'btnFormat',
          value: CUSTOM_BTN_DEFAULT,
        },
      ],
    },
    {
      label: 'customBtn.btnFontGroup',
      key: 'btnFontGroup',
      comType: 'group',
      rows: [
        {
          label: 'customBtn.btnFont',
          key: 'btnFont',
          comType: 'font',
          value: FONT_DEFAULT,
        },
      ],
    },
  ];
};
const customBtnI18N = {
  zh: {
    meta: '按钮属性',
    btnFontGroup: '按钮字体',
    btnFormat: '按钮属性',
    btnFont: '按钮字体',
    content: '按钮文字',
    btnType: '按钮类型',
    danger: '危险操作',
    icon: '按钮图标',
    iconPlaceholder: 'Ant Design 图标',
    btnSize: '按钮大小',
    jumpType: '跳转目标',
    target: '打开方式',
    dashboard: '仪表板',
    datachart: '图表',
    newWindow: '新窗口',
    currentPage: '当前页',
    default: '默认',
    primary: '主要',
    text: '文字',
    link: '链接',
    dashed: '虚线',
    large: '大',
    middle: '中',
    small: '小',
  },
  en: {
    meta: 'Button Props',
    btnFontGroup: 'Button Font',
    btnFormat: 'Button properties',
    btnFont: 'Button font',
    content: 'Button text',
    btnType: 'Button type',
    danger: 'Danger style',
    icon: 'Button icon',
    iconPlaceholder: 'Ant Design icon',
    btnSize: 'Button size',
    jumpType: 'Navigation target',
    target: 'Open in',
    dashboard: 'Dashboard',
    datachart: 'Data chart',
    newWindow: 'New window',
    currentPage: 'Current page',
    default: 'Default',
    primary: 'Primary',
    text: 'Text',
    link: 'Link',
    dashed: 'Dashed',
    large: 'Large',
    middle: 'Medium',
    small: 'Small',
  },
};
const NameI18N = {
  zh: '按钮',
  en: 'Button',
};
export const widgetMeta: WidgetMeta = {
  icon: 'query-widget',
  originalType: ORIGINAL_TYPE_MAP.customBtn,
  canWrapped: true,
  controllable: false,
  linkable: false,
  canFullScreen: true,
  singleton: false,

  i18ns: [
    {
      lang: 'zh-CN',
      translation: {
        ...InteractionI18N.zh,
        desc: 'customBtn',
        widgetName: NameI18N.zh,
        action: {},
        title: TitleI18N.zh,
        customBtn: customBtnI18N.zh,
        background: { backgroundGroup: '背景' },
        padding: PaddingI18N.zh,
        border: { borderGroup: '边框' },
      },
    },
    {
      lang: 'en-US',
      translation: {
        ...InteractionI18N.en,
        desc: 'customBtn',
        widgetName: NameI18N.en,
        action: {},
        title: TitleI18N.en,
        background: { backgroundGroup: 'Background' },
        padding: PaddingI18N.en,
        customBtn: customBtnI18N.en,
        border: { borderGroup: 'Border' },
      },
    },
  ],
};
export interface CustomBtnWidgetToolKit extends WidgetToolkit {
  getCustomBtnConfig: (props) => ICustomBtnDefault;
  getCustomBtnFont: (props) => IFontDefault;
}
export const widgetToolkit: CustomBtnWidgetToolKit = {
  create: opt => {
    const widget = widgetTpl();
    widget.id = widgetMeta.originalType + widget.id;
    widget.parentId = opt.parentId || '';
    widget.viewIds = opt.viewIds || [];
    widget.relations = opt.relations || [];
    widget.config.originalType = widgetMeta.originalType;
    widget.config.type = 'media';
    widget.config.name = opt.name || '';
    widget.config.rect.width = 100;
    widget.config.rect.height = 60;
    widget.config.pRect.width = 2;
    widget.config.pRect.height = 1;

    widget.config.customConfig.props = [
      ...initCustomBtnTpl(),
      // { ...initTitleTpl() },
      { ...initBackgroundTpl(WHITE) },
      { ...initPaddingTpl({ top: 0, bottom: 0, left: 0, right: 0 }) },
      { ...initBorderTpl() },
    ];

    widget.config.customConfig.interactions = [...initInteractionTpl()];

    return widget;
  },
  getName(key) {
    return initWidgetName(NameI18N, key);
  },
  edit() {},
  save() {},
  getDropDownList(...arg) {
    const list: WidgetActionListItem<widgetActionType>[] = [
      {
        key: 'edit',
        renderMode: ['edit'],
      },
      {
        key: 'delete',
        renderMode: ['edit'],
      },
      {
        key: 'lock',
        renderMode: ['edit'],
      },
      {
        key: 'group',
        renderMode: ['edit'],
      },
    ];
    return list;
  },
  getCustomBtnConfig(props) {
    const [btnFormat] = getJsonConfigs(props, ['metaGroup'], ['btnFormat']) as [
      ICustomBtnDefault,
    ];
    return btnFormat;
  },
  getCustomBtnFont(props) {
    const [btnFont] = getJsonConfigs(props, ['btnFontGroup'], ['btnFont']) as [
      IFontDefault,
    ];
    return btnFont;
  },
  // lock() {},
  // unlock() {},
  // copy() {},
  // paste() {},
  // delete() {},
  // changeTitle() {},
  // getMeta() {},
  // getWidgetName() {},
  // //
};

const customBtnProto: WidgetProto = {
  originalType: widgetMeta.originalType,
  meta: widgetMeta,
  toolkit: widgetToolkit,
};
export const customBtnWidgetToolKit = widgetToolkit;
export default customBtnProto;
