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

import { Form, InputNumber, message, Radio, Select, Space, Tooltip } from 'antd';
import { ChartStyleConfig } from 'app/types/ChartConfig';
import { FC, memo, useEffect, useState } from 'react';
import styled from 'styled-components/macro';
import { isEmptyArray } from 'utils/object';
import { InteractionDialogType, InteractionFieldMapper, InteractionMouseEvent } from '../../constants';
import { ItemLayoutProps } from '../../types';
import { itemLayoutComparer } from '../../utils';
import { DialogSizeConfig, ViewDetailSetting } from './types';

const ViewDetailPanel: FC<ItemLayoutProps<ChartStyleConfig>> = memo(
  ({ ancestors, translate: t = title => title, data, context, onChange }) => {
    const [event, setEvent] = useState<ViewDetailSetting['event']>(
      data.value?.event || InteractionMouseEvent.Left,
    );
    const [mapper, setMapper] = useState<ViewDetailSetting['mapper']>(
      data?.value?.mapper,
    );
    const [customFields, setCustomFields] = useState<string[]>(
      data?.value?.[InteractionFieldMapper.Customize] || [],
    );
    const [selectedRatio, setSelectedRatio] = useState<string>('middle');
    const [dialogSizeConfig, setDialogSizeConfig] = useState<
      ViewDetailSetting['dialogSize']
    >(
      data.value?.dialogSize || {
        configType: InteractionDialogType.Ratio,
        dialogSize: {
          weight: 1000,
          height: 400,
          contentHeight: 400,
        },
      },
    );
    const ratioType = [
      {
        value: 'small',
        label: t('drillThrough.rule.dialogSizeConfig.ratioType.small'),
      },
      {
        value: 'middle',
        label: t('drillThrough.rule.dialogSizeConfig.ratioType.middle'),
      },
      {
        value: 'big',
        label: t('drillThrough.rule.dialogSizeConfig.ratioType.big'),
      },
    ];

    const ratioSize = {
      small: {
        weight: 800,
        height: 200,
        contentHeight: 200,
      },
      middle: {
        weight: 1000,
        height: 400,
        contentHeight: 400,
      },
      big: {
        weight: 1400,
        height: 600,
        contentHeight: 600,
      },
    };

    useEffect(() => {
      // 根据传入的 value 来判断选择哪个大小
      if (dialogSizeConfig?.dialogSize) {
        const { weight, height, contentHeight } = dialogSizeConfig.dialogSize;
        // 根据 weight, height 和 contentHeight 来判断
        for (const size in ratioSize) {
          if (
            ratioSize[size].weight === weight &&
            ratioSize[size].height === height &&
            ratioSize[size].contentHeight === contentHeight
          ) {
            setSelectedRatio(size); // 设置对应的 size
            break;
          }
        }
      }
    }, [dialogSizeConfig?.dialogSize]);

    const handleViewDetailEventChange = e => {
      const event = e.target.value;
      handleViewDetailSettingChange(event);
    };

    const handleViewDetailMapperChange = e => {
      const mapper = e.target.value;
      handleViewDetailSettingChange(undefined, mapper);
    };

    const handleViewDetailCustomFieldsChange = values => {
      handleViewDetailSettingChange(undefined, undefined, values);
    };

    const handleViewDetailDialogSizeChange = value => {
      handleViewDetailSettingChange(undefined, undefined, undefined, value);
    };

    const handleViewDetailSettingChange = (
      newEvent?: InteractionMouseEvent,
      newMapper?: InteractionFieldMapper,
      customFields?: string[],
      newDialogSize?: DialogSizeConfig,
    ) => {
      let newSetting: ViewDetailSetting = {
        event: newEvent || event,
        mapper: newMapper || mapper,
        [InteractionFieldMapper.Customize]: customFields,
        dialogSize: newDialogSize || dialogSizeConfig,
      };
      if (newEvent) {
        setEvent(newEvent);
      }
      if (newMapper) {
        setMapper(newMapper);
      }
      if (!isEmptyArray(customFields)) {
        setCustomFields(customFields!);
      }
      if (newDialogSize) {
        setDialogSizeConfig(newDialogSize);
      }
      onChange?.(ancestors, newSetting, false);
    };

    return (
      <StyledDrillThroughPanel direction="vertical">
        <Form
          labelCol={{ offset: 2, span: 2 }}
          wrapperCol={{ span: 18 }}
          layout="horizontal"
          size="middle"
          initialValues={{
            event,
            mapper,
            custom: customFields,
            dialogSizeConfig,
          }}
        >
          <Form.Item label={t('viewDetail.event')} name="event">
            <Radio.Group onChange={handleViewDetailEventChange}>
              <Radio value={InteractionMouseEvent.Left}>
                {t('viewDetail.leftClick')}
              </Radio>
              <Radio value={InteractionMouseEvent.Right}>
                {t('viewDetail.rightClick')}
              </Radio>
            </Radio.Group>
          </Form.Item>
          <Form.Item label={t('viewDetail.field')} name="mapper">
            <Radio.Group onChange={handleViewDetailMapperChange}>
              <Radio value={InteractionFieldMapper.All}>
                {t('viewDetail.all')}
              </Radio>
              <Radio value={InteractionFieldMapper.Customize}>
                {t('viewDetail.customize')}
              </Radio>
            </Radio.Group>
          </Form.Item>
          {mapper === InteractionFieldMapper.Customize && (
            <Form.Item label=" " colon={false} name="custom">
              <Select
                mode="multiple"
                optionFilterProp="children"
                allowClear
                onChange={handleViewDetailCustomFieldsChange}
              >
                {context?.dataview?.meta
                  ?.flatMap(f => {
                    if (f.role === 'hierachy') {
                      return f.children || [];
                    }
                    return f;
                  })
                  ?.map(f => {
                    return (
                      <Select.Option value={f.name}>{f.name}</Select.Option>
                    );
                  })}
              </Select>
            </Form.Item>
          )}
          <Form.Item
            label={t('viewDetail.dialogSizeConfig')}
            name="dialogSizeConfig"
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '10px', // 控制每个元素之间的间距
                marginTop: 10,
              }}
            >
              {/* Radio.Group */}
              <Radio.Group
                size="small"
                style={{
                  display: 'flex',
                  flexDirection: 'column', // 垂直排列
                  flexShrink: 0, // 防止被挤压
                  marginLeft: 5,
                }}
                value={dialogSizeConfig?.configType}
                onChange={e => {
                  if (e.target.value === InteractionDialogType.Ratio) {
                    handleViewDetailDialogSizeChange({
                      dialogSize: ratioSize[selectedRatio],
                      configType: e.target.value,
                    });
                  } else {
                    handleViewDetailDialogSizeChange({
                      dialogSize: ratioSize['middle'],
                      configType: e.target.value,
                    });
                  }
                }}
              >
                <Radio value={InteractionDialogType.Ratio}>
                  {t('drillThrough.rule.dialogSizeConfig.ratio')}
                </Radio>
                <Radio value={InteractionDialogType.Customize}>
                  {t('drillThrough.rule.dialogSizeConfig.customize')}
                </Radio>
              </Radio.Group>
              {dialogSizeConfig?.configType === InteractionDialogType.Ratio && (
                <Select
                  style={{ width: 150 }}
                  options={ratioType}
                  value={selectedRatio} // 设置默认值
                  onChange={newValue =>
                    handleViewDetailDialogSizeChange({
                      dialogSize: ratioSize[newValue],
                      configType: InteractionDialogType.Ratio,
                    })
                  }
                />
              )}
              {dialogSizeConfig?.configType ===
                InteractionDialogType.Customize && (
                  <>
                    <Tooltip placement="topLeft" title={t('drillThrough.rule.dialogSizeConfig.widthRatio')}>
                      <div
                        style={{ display: 'inline-flex', alignItems: 'center' }}
                      >
                        <InputNumber
                          style={{ width: 75 }}
                          value={dialogSizeConfig?.dialogSize?.weight}
                          placeholder={t('drillThrough.rule.dialogSizeConfig.widthRatio')}
                          onChange={value => {
                            const weight = Number(value);
                            if (!isNaN(weight) && weight > 0) {
                              handleViewDetailDialogSizeChange({
                                dialogSize: {
                                  ...dialogSizeConfig.dialogSize,
                                  weight,
                                },
                                configType: InteractionDialogType.Customize,
                              });
                            } else {
                              message.warn(t('drillThrough.rule.dialogSizeConfig.widthTips'));
                            }
                          }}
                        />
                        <span
                          style={{
                            border: '1px solid #d9d9d9',
                            backgroundColor: '#f0f0f0',
                            padding: '0 8px',
                            height: '32px',
                            display: 'flex',
                            justifyContent: 'center',
                            alignItems: 'center',
                            fontSize: '14px',
                          }}
                        >
                        PX
                      </span>
                      </div>
                    </Tooltip>
                    <Tooltip placement="topLeft" title={t('drillThrough.rule.dialogSizeConfig.dialogHeight')}>
                      <div
                        style={{ display: 'inline-flex', alignItems: 'center' }}
                      >
                        <InputNumber
                          style={{ width: 75 }}
                          value={dialogSizeConfig.dialogSize?.height}
                          placeholder={t('drillThrough.rule.dialogSizeConfig.dialogHeight')}
                          onChange={value => {
                            const height = Number(value);
                            if (!isNaN(height) && height > 0) {
                              handleViewDetailDialogSizeChange({
                                dialogSize: {
                                  ...dialogSizeConfig.dialogSize,
                                  height,
                                },
                                configType: InteractionDialogType.Customize,
                              });
                            } else {
                              message.warn(t('drillThrough.rule.dialogSizeConfig.heightTips'));
                            }
                          }}
                        />
                        <span
                          style={{
                            border: '1px solid #d9d9d9',
                            backgroundColor: '#f0f0f0',
                            padding: '0 8px',
                            height: '32px',
                            display: 'flex',
                            justifyContent: 'center',
                            alignItems: 'center',
                            fontSize: '14px',
                          }}
                        >
                        PX
                      </span>
                      </div>
                    </Tooltip>
                    <Tooltip placement="topLeft" title={t('drillThrough.rule.dialogSizeConfig.contentHeight')}>
                      <div
                        style={{ display: 'inline-flex', alignItems: 'center' }}
                      >
                        <InputNumber
                          style={{ width: 75 }}
                          value={dialogSizeConfig.dialogSize?.contentHeight}
                          placeholder={t('drillThrough.rule.dialogSizeConfig.contentHeight')}
                          onChange={value => {
                            const contentHeight = Number(value);
                            const dialogHeight =
                              dialogSizeConfig.dialogSize?.height || 0;
                            if (!isNaN(contentHeight) && contentHeight > 0) {
                              if (contentHeight > dialogHeight) {
                                message.warn(t('drillThrough.rule.dialogSizeConfig.contentTips2'));
                              } else {
                                handleViewDetailDialogSizeChange({
                                  dialogSize: {
                                    ...dialogSizeConfig.dialogSize,
                                    contentHeight,
                                  },
                                  configType: InteractionDialogType.Customize,
                                });
                              }
                            } else {
                              message.warn(t('drillThrough.rule.dialogSizeConfig.contentTips1'));
                            }
                          }}
                        />
                        <span
                          style={{
                            border: '1px solid #d9d9d9',
                            backgroundColor: '#f0f0f0',
                            padding: '0 8px',
                            height: '32px',
                            display: 'flex',
                            justifyContent: 'center',
                            alignItems: 'center',
                            fontSize: '14px',
                          }}
                        >
                        PX
                      </span>
                      </div>
                    </Tooltip>
                  </>
                )}
            </div>
          </Form.Item>
        </Form>
      </StyledDrillThroughPanel>
    );
  },
  itemLayoutComparer,
);

export default ViewDetailPanel;

const StyledDrillThroughPanel = styled(Space)`
  width: 100%;
`;
