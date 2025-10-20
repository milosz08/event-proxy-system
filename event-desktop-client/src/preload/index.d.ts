declare global {
  interface Window {
    api: {
      sendPing: () => Promise<string>;
      clearPings: () => Promise<string>;
      onPong: (callback: (message: string) => void) => () => void;
      onClearedPings: (callback: () => void) => () => void;
    };
  }
}

export {};
