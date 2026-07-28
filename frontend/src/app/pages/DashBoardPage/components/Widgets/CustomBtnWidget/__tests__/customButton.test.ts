import customBtnProto from '../customBtnConfig';
import { getCustomButtonNavigation } from '../navigation';

describe('custom dashboard button', () => {
  test('creates a registered media widget with stable defaults', () => {
    const widget = customBtnProto.toolkit.create({ parentId: 'parent-1' });

    expect(customBtnProto.originalType).toBe('customBtn');
    expect(widget.parentId).toBe('parent-1');
    expect(widget.config).toEqual(
      expect.objectContaining({
        originalType: 'customBtn',
        type: 'media',
      }),
    );
    expect(widget.config.rect).toEqual(
      expect.objectContaining({ width: 100, height: 60 }),
    );
    const english = customBtnProto.meta.i18ns.find(
      i18n => i18n.lang === 'en-US',
    )?.translation as any;
    expect(english.customBtn).toEqual(
      expect.objectContaining({
        content: 'Button text',
        jumpType: 'Navigation target',
        target: 'Open in',
      }),
    );
  });

  test('resolves internal dashboard and data-chart targets', () => {
    expect(
      getCustomButtonNavigation({
        jumpType: 'dashboard',
        dashboardId: 'dashboard-1',
        target: '_self',
      }),
    ).toEqual({ kind: 'internal', relId: 'dashboard-1', target: '_self' });
    expect(
      getCustomButtonNavigation({
        jumpType: 'datachart',
        datachartId: 'chart-1',
        target: '_blank',
      }),
    ).toEqual({ kind: 'internal', relId: 'chart-1', target: '_blank' });
  });

  test('resolves URL targets and ignores incomplete configuration', () => {
    expect(
      getCustomButtonNavigation({
        jumpType: 'url',
        href: 'https://example.com/report',
        target: '_blank',
      }),
    ).toEqual({
      kind: 'url',
      href: 'https://example.com/report',
      target: '_blank',
    });
    expect(getCustomButtonNavigation({ jumpType: 'url' })).toBeUndefined();
    expect(getCustomButtonNavigation({})).toBeUndefined();
  });
});
