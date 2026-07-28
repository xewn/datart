import {
  DEFAULT_INTERACTION_DIALOG_SIZE,
  DEFAULT_INTERACTION_DIALOG_SIZE_CONFIG,
  normalizeInteractionDialogSize,
  normalizeInteractionDialogSizeConfig,
} from '../dialogSize';

describe('interaction dialog size', () => {
  test('uses one default size for every interaction entry point', () => {
    expect(normalizeInteractionDialogSize()).toEqual(
      DEFAULT_INTERACTION_DIALOG_SIZE,
    );
    expect(DEFAULT_INTERACTION_DIALOG_SIZE).toEqual({
      weight: 80,
      height: 600,
      contentHeight: 600,
    });
  });

  test('bounds width and content height before rendering a dialog', () => {
    expect(
      normalizeInteractionDialogSize({
        weight: 120,
        height: 400,
        contentHeight: 900,
      }),
    ).toEqual({
      weight: 100,
      height: 400,
      contentHeight: 400,
    });
  });

  test('replaces non-positive and non-finite dimensions with defaults', () => {
    expect(
      normalizeInteractionDialogSize({
        weight: 0,
        height: Number.NaN,
        contentHeight: -1,
      }),
    ).toEqual(DEFAULT_INTERACTION_DIALOG_SIZE);
  });

  test('upgrades partial persisted dialog configuration', () => {
    expect(normalizeInteractionDialogSizeConfig({})).toEqual(
      DEFAULT_INTERACTION_DIALOG_SIZE_CONFIG,
    );
  });
});
