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

import { normalizeInteractionDialogSize } from 'app/components/FormGenerator/Customize/Interaction/dialogSize';
import useStateModal from 'app/hooks/useStateModal';
import { VizContainer } from '../Main/VizContainer';

const useDisplayJumpVizDialog = () => {
  const [openStateModal, contextHolder] = useStateModal({});
  const openJumpVizDialogModal = ({
    orgId,
    vizId,
    vizType,
    params,
    dialogSize,
  }) => {
    const ds = normalizeInteractionDialogSize(dialogSize);
    return (openStateModal as Function)({
      modalSize: (window.innerWidth * ds.weight) / 100,
      content: () => (
        <div style={{ height: ds.height }}>
          <VizContainer
            tab={{ id: vizId, type: vizType, search: params } as any}
            orgId={orgId}
            vizs={[]}
            selectedId={vizId}
            hideTitle={true}
          />
        </div>
      ),
    }) as VoidFunction;
  };

  return [openJumpVizDialogModal, contextHolder];
};

export default useDisplayJumpVizDialog;
