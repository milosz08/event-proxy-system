import { useCallback, useState } from 'react';

const useSpinner = (): [
  boolean,
  (preCheck: () => boolean, action: () => Promise<void>) => Promise<void>,
] => {
  const [loading, setLoading] = useState(false);

  const run = useCallback(async (preCheck: () => boolean, action: () => Promise<void>) => {
    if (preCheck()) {
      setLoading(true);
      await action();
      setLoading(false);
    }
  }, []);

  return [loading, run];
};

export default useSpinner;
