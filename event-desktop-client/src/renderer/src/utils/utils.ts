// "Feb 7, 2026, 23:35:36"
export const formatTimestamp = (timestamp?: number | string): string => {
  if (!timestamp) {
    return '?';
  }
  const options = {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  } as const;
  return new Date(timestamp).toLocaleString('en-US', options);
};
