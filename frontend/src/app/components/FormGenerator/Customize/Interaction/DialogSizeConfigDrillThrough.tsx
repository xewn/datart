import React, { useEffect, useState } from 'react';
import { InputNumber, message, Radio, Select, Tooltip } from 'antd';
import { InteractionDialogType } from '../../constants';
import { I18nTranslator } from './types';

interface DialogSizeConfigProps {
  value: any;
  recordId: string;
  onRuleChange: (id: string, prop: string, value: any) => void;
  translate: I18nTranslator['translate'];
}

const DialogSizeConfigDrillThrough: React.FC<DialogSizeConfigProps> = ({
  value,
  recordId,
  onRuleChange,
  translate: t,
}) => {
  const [selectedRatio, setSelectedRatio] = useState<string>('middle');
  useEffect(() => {
    // 根据传入的 value 来判断选择哪个大小
    if (value.dialogSize) {
      const { weight, height, contentHeight } = value.dialogSize;
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
  }, [value.dialogSize]);

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
      weight: 70,
      height: 500,
      contentHeight: 500,
    },
    middle: {
      weight: 80,
      height: 600,
      contentHeight: 600,
    },
    big: {
      weight: 90,
      height: 800,
      contentHeight: 770,
    },
  };
  return (
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
        value={value.configType}
        onChange={e => {
          if (e.target.value === InteractionDialogType.Ratio) {
            onRuleChange(recordId, 'dialogSize', {
              dialogSize: ratioSize[selectedRatio],
              configType: e.target.value,
            });
          } else {
            onRuleChange(recordId, 'dialogSize', {
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
      {value.configType === InteractionDialogType.Ratio && (
        <Select
          style={{ width: 150 }}
          options={ratioType}
          value={selectedRatio} // 设置默认值
          onChange={newValue =>
            onRuleChange(recordId, 'dialogSize', {
              dialogSize: ratioSize[newValue],
              configType: InteractionDialogType.Ratio,
            })
          }
        />
      )}
      {value.configType === InteractionDialogType.Customize && (
        <>
          <Tooltip placement="topLeft" title={t('drillThrough.rule.dialogSizeConfig.widthRatio')}>
            <div style={{ display: 'inline-flex', alignItems: 'center' }}>
              <InputNumber
                style={{ width: 65 }}
                value={value.dialogSize?.weight}
                placeholder={t('drillThrough.rule.dialogSizeConfig.widthRatio')}
                onBlur={e => {
                  const v = e.target.value;
                  const weight = Number(v);
                  if (
                    !isNaN(weight) &&
                    weight > 0 &&
                    weight <= 100 // 校验规则
                  ) {
                    onRuleChange(recordId, 'dialogSize', {
                      dialogSize: {
                        ...value.dialogSize,
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
                %
              </span>
            </div>
          </Tooltip>
          <Tooltip placement="topLeft" title={t('drillThrough.rule.dialogSizeConfig.dialogHeight')}>
            <div style={{ display: 'inline-flex', alignItems: 'center' }}>
              <InputNumber
                style={{ width: 75 }}
                value={value.dialogSize?.height}
                placeholder={t('drillThrough.rule.dialogSizeConfig.dialogHeight')}
                onBlur={e => {
                  const v = e.target.value;
                  const height = Number(v);
                  const { dialogSize } = value;
                  if (!isNaN(height) && height > 0) {
                    onRuleChange(recordId, 'dialogSize', {
                      dialogSize: {
                        ...dialogSize,
                        contentHeight: Math.min(
                          dialogSize.contentHeight,
                          height,
                        ),
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
            <div style={{ display: 'inline-flex', alignItems: 'center' }}>
              <InputNumber
                style={{ width: 75 }}
                value={value.dialogSize?.contentHeight}
                placeholder={t('drillThrough.rule.dialogSizeConfig.contentHeight')}
                onBlur={e => {
                  const v = e.target.value;
                  const contentHeight = Number(v);
                  const dialogHeight = value.dialogSize?.height || 0;
                  if (!isNaN(contentHeight) && contentHeight > 0) {
                    if (contentHeight > dialogHeight) {
                      message.warn(t('drillThrough.rule.dialogSizeConfig.contentTips2'));
                    } else {
                      onRuleChange(recordId, 'dialogSize', {
                        dialogSize: {
                          ...value.dialogSize,
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
  );
};

export default DialogSizeConfigDrillThrough;
