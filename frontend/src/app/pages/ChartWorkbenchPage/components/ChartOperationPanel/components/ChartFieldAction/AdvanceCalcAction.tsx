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

import { CheckOutlined } from '@ant-design/icons';
import { Menu } from 'antd';
import {
  AdvanceCalcFieldActionType,
  DataViewFieldType,
  DateLevelTypes,
} from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { ChartDataSectionField } from 'app/types/ChartConfig';
import { ChartDataViewMeta } from 'app/types/ChartDataViewMeta';
import { updateBy } from 'app/utils/mutation';
import { FC } from 'react';

export const canUseDateRatio = (
  metas?: ChartDataViewMeta[],
  availableSourceFunctions?: string[],
) =>
  Boolean(
    metas?.some(meta => meta.type === DataViewFieldType.DATE) &&
      availableSourceFunctions?.some(fn => DateLevelTypes.includes(fn)),
  );

const AdvanceCalcAction: FC<{
  uid?: string;
  config: ChartDataSectionField;
  onConfigChange: (
    config: ChartDataSectionField,
    needRefresh?: boolean,
  ) => void;
  onOpenModal;
  metas?: ChartDataViewMeta[];
  availableSourceFunctions?: string[];
}> = ({
  uid,
  config,
  onConfigChange,
  onOpenModal,
  metas,
  availableSourceFunctions,
}) => {
  const t = useI18NPrefix('viz.common.enum.advanceCalc');

  return (
    <>
      <Menu.Item
        key={AdvanceCalcFieldActionType.None}
        eventKey={AdvanceCalcFieldActionType.None}
        icon={!config.calc ? <CheckOutlined /> : undefined}
        onClick={() => {
          const newConfig = updateBy(config, draft => {
            draft.calc = undefined;
          });
          onConfigChange(newConfig, true);
        }}
      >
        {t(AdvanceCalcFieldActionType.None)}
      </Menu.Item>
      {canUseDateRatio(metas, availableSourceFunctions) && (
        <Menu.Item
          key={AdvanceCalcFieldActionType.Ratio}
          eventKey={AdvanceCalcFieldActionType.Ratio}
          icon={
            config.calc?.type === AdvanceCalcFieldActionType.Ratio ? (
              <CheckOutlined />
            ) : undefined
          }
          onClick={() => onOpenModal(uid)(AdvanceCalcFieldActionType.Ratio)}
        >
          {t(AdvanceCalcFieldActionType.Ratio)}
        </Menu.Item>
      )}
    </>
  );
};

export default AdvanceCalcAction;
