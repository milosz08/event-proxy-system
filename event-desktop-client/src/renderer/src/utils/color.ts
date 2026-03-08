import { getStringHash } from '@renderer/utils/utils';

type Colors = {
  base: string;
  hover: string;
};

const colorCache = new Map<string, Colors>();

export const getEventSourceColors = (source: string | undefined): Colors => {
  if (!source) {
    return {
      base: 'transparent',
      hover: 'rgba(125, 125, 125, 0.15)',
    };
  }
  if (colorCache.has(source)) {
    return colorCache.get(source)!;
  }
  const hue = getStringHash(source) % 360;
  const colors = {
    base: `hsla(${hue}, 80%, 65%, 0.15)`,
    hover: `hsla(${hue}, 80%, 65%, 0.25)`,
  };
  colorCache.set(source, colors);
  return colors;
};
