import { Input, Radio, Select, Space, Switch, Typography } from 'antd';
import { FC, memo } from 'react';
import { IconList } from '../../../pages/DashBoardPage/components/Widgets/CustomBtnWidget/util/Icon';
import { ChartStyleConfig } from '../../../types/ChartConfig';
import { updateByKey } from '../../../utils/mutation';
import { ItemLayoutProps } from '../types';

const BtnFormat: FC<ItemLayoutProps<ChartStyleConfig>> = memo(
  ({ ancestors, translate: t = title => title, data, onChange, context }) => {
    const TextSecondary = props => (
      <Typography.Text type={'secondary'} {...props} />
    );

    const handleSettingChange = key => e => {
      const val = e.target.value;
      handleSettingChangeWithValue(key)(val);
    };

    const handleSettingChangeWithValue = key => val => {
      const newData = updateByKey(
        data,
        'value',
        Object.assign({}, data.value, { [key]: val }),
      );
      onChange?.(ancestors, newData);
    };

    const BtnTypes = [
      {
        name: t('customBtn.default'),
        value: 'default',
      },
      {
        name: t('customBtn.primary'),
        value: 'primary',
      },
      {
        name: t('customBtn.text'),
        value: 'text',
      },
      {
        name: t('customBtn.link'),
        value: 'link',
      },
      {
        name: t('customBtn.dashed'),
        value: 'dashed',
      },
    ];

    const BtnSizes = [
      {
        name: t('customBtn.large'),
        value: 'large',
      },
      {
        name: t('customBtn.middle'),
        value: 'middle',
      },
      {
        name: t('customBtn.small'),
        value: 'small',
      },
    ];

    return (
      <Space direction={'vertical'}>
        <Space direction={'vertical'}>
          <TextSecondary>{t('customBtn.content')}</TextSecondary>
          <Input
            value={data.value?.content}
            style={{ width: '100%' }}
            onChange={handleSettingChange('content')}
            className={'datart-antd-input'}
          />
        </Space>
        <Space direction={'vertical'}>
          <TextSecondary>{t('customBtn.btnType')}</TextSecondary>
          <Select
            value={data.value?.btnType}
            style={{ minWidth: '100%' }}
            onChange={handleSettingChangeWithValue('btnType')}
          >
            {BtnTypes.map(ele => (
              <Select.Option key={ele.value} value={ele.value}>
                {ele.name}
              </Select.Option>
            ))}
          </Select>
        </Space>
        <Space direction={'vertical'}>
          <TextSecondary>{t('customBtn.danger')}</TextSecondary>
          <Switch
            checked={data.value?.danger}
            style={{ minWidth: '100%' }}
            onChange={handleSettingChangeWithValue('danger')}
          />
        </Space>
        <Space direction={'vertical'}>
          <TextSecondary>{t('customBtn.icon')}</TextSecondary>
          <Select
            value={data.value?.icon}
            placeholder={t('customBtn.iconPlaceholder')}
            style={{ minWidth: 150 }}
            showSearch={true}
            onChange={handleSettingChangeWithValue('icon')}
            options={IconList}
          />
        </Space>
        <Space direction={'vertical'}>
          <TextSecondary>{t('customBtn.btnSize')}</TextSecondary>
          <Select
            value={data.value?.btnSize}
            style={{ minWidth: '100%' }}
            onChange={handleSettingChangeWithValue('btnSize')}
          >
            {BtnSizes.map(ele => (
              <Select.Option key={ele.value} value={ele.value}>
                {ele.name}
              </Select.Option>
            ))}
          </Select>
        </Space>
        <Space direction={'vertical'}>
          <TextSecondary>{t('customBtn.jumpType')}</TextSecondary>
          <Radio.Group
            value={data.value?.jumpType}
            onChange={handleSettingChange('jumpType')}
          >
            <Radio.Button value={'url'}>URL</Radio.Button>
            <Radio.Button value={'dashboard'}>
              {t('customBtn.dashboard')}
            </Radio.Button>
            <Radio.Button value={'datachart'}>
              {t('customBtn.datachart')}
            </Radio.Button>
          </Radio.Group>
        </Space>
        {data.value?.jumpType && (
          <Space direction={'vertical'}>
            <TextSecondary>{t('customBtn.target')}</TextSecondary>
            <Radio.Group
              value={data.value?.target}
              onChange={handleSettingChange('target')}
            >
              <Radio.Button value={'_blank'}>
                {t('customBtn.newWindow')}
              </Radio.Button>
              <Radio.Button value={'_self'}>
                {t('customBtn.currentPage')}
              </Radio.Button>
            </Radio.Group>
          </Space>
        )}
        {data.value?.jumpType === 'url' && (
          <Space direction={'vertical'}>
            <TextSecondary>URL</TextSecondary>
            <Input.TextArea
              value={data.value?.href}
              style={{ minWidth: '100%' }}
              onChange={handleSettingChange('href')}
              className={'datart-antd-input'}
            />
          </Space>
        )}
        {data.value?.jumpType === 'dashboard' && (
          <Space direction={'vertical'}>
            <TextSecondary>{t('customBtn.dashboard')}</TextSecondary>
            <Select
              virtual
              showSearch
              value={data.value?.dashboardId}
              style={{ minWidth: 100, maxWidth: 200 }}
              dropdownMatchSelectWidth={false}
              onChange={handleSettingChangeWithValue('dashboardId')}
              className={'datart-antd-input'}
            >
              {context?.vizs
                ?.filter(v => v.relType === 'DASHBOARD')
                ?.map(c => {
                  return (
                    <Select.Option key={c.relId} value={c.relId}>
                      {c.name}
                    </Select.Option>
                  );
                })}
            </Select>
          </Space>
        )}
        {data.value?.jumpType === 'datachart' && (
          <Space direction={'vertical'}>
            <TextSecondary>{t('customBtn.datachart')}</TextSecondary>
            <Select
              virtual
              showSearch
              value={data.value?.datachartId}
              style={{ minWidth: 100, maxWidth: 200 }}
              dropdownMatchSelectWidth={false}
              onChange={handleSettingChangeWithValue('datachartId')}
              className={'datart-antd-input'}
            >
              {context?.vizs
                ?.filter(v => v.relType === 'DATACHART')
                ?.map(c => {
                  return (
                    <Select.Option key={c.relId} value={c.relId}>
                      {c.name}
                    </Select.Option>
                  );
                })}
            </Select>
          </Space>
        )}
      </Space>
    );
  },
);

export default BtnFormat;
