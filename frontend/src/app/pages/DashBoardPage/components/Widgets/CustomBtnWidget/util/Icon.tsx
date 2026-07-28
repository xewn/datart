import * as icons from '@ant-design/icons';
import React from 'react';

export const Icon = (props: { icon: string }) => {
  const { icon } = props;
  if (icons[icon] === undefined) return null;
  const antIcon: { [key: string]: any } = icons;
  return React.createElement(antIcon[icon]);
};

export const IconList = Object.keys(icons).map(icon => {
  return {
    key: icon,
    value: icon,
  };
});
