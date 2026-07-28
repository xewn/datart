import { InteractionDialogType } from '../../constants';
import { DialogSize, DialogSizeConfig } from './types';

export const INTERACTION_DIALOG_SIZE_PRESETS = {
  small: { weight: 70, height: 500, contentHeight: 500 },
  middle: { weight: 80, height: 600, contentHeight: 600 },
  big: { weight: 90, height: 800, contentHeight: 770 },
};

export const DEFAULT_INTERACTION_DIALOG_SIZE: DialogSize = {
  ...INTERACTION_DIALOG_SIZE_PRESETS.middle,
};

export const DEFAULT_INTERACTION_DIALOG_SIZE_CONFIG: DialogSizeConfig = {
  configType: InteractionDialogType.Ratio,
  dialogSize: DEFAULT_INTERACTION_DIALOG_SIZE,
};

const positiveOrDefault = (value: number | undefined, fallback: number) =>
  Number.isFinite(value) && Number(value) > 0 ? Number(value) : fallback;

export const normalizeInteractionDialogSize = (
  value?: Partial<DialogSize>,
): DialogSize => {
  const weight = Math.min(
    100,
    positiveOrDefault(value?.weight, DEFAULT_INTERACTION_DIALOG_SIZE.weight),
  );
  const height = positiveOrDefault(
    value?.height,
    DEFAULT_INTERACTION_DIALOG_SIZE.height,
  );
  const contentHeight = Math.min(
    height,
    positiveOrDefault(
      value?.contentHeight,
      DEFAULT_INTERACTION_DIALOG_SIZE.contentHeight,
    ),
  );
  return { weight, height, contentHeight };
};

export const normalizeInteractionDialogSizeConfig = (
  value?: Partial<DialogSizeConfig>,
): DialogSizeConfig => ({
  configType: value?.configType || InteractionDialogType.Ratio,
  dialogSize: normalizeInteractionDialogSize(value?.dialogSize),
});
