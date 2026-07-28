import { ICustomBtnDefault } from 'types';

type CustomButtonNavigation =
  | { kind: 'internal'; relId: string; target: '_self' | '_blank' }
  | { kind: 'url'; href: string; target: '_self' | '_blank' };

const normalizeTarget = (target?: ICustomBtnDefault['target']) =>
  target === '_blank' ? '_blank' : '_self';

export const getCustomButtonNavigation = (
  config: Partial<ICustomBtnDefault>,
): CustomButtonNavigation | undefined => {
  const target = normalizeTarget(config.target);
  if (config.jumpType === 'dashboard' && config.dashboardId) {
    return { kind: 'internal', relId: config.dashboardId, target };
  }
  if (config.jumpType === 'datachart' && config.datachartId) {
    return { kind: 'internal', relId: config.datachartId, target };
  }
  if (config.jumpType === 'url' && config.href?.trim()) {
    return { kind: 'url', href: config.href.trim(), target };
  }
  return undefined;
};
