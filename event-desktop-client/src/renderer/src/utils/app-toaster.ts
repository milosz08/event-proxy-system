import { Intent, OverlayToaster, Position, ToastProps } from '@blueprintjs/core';

const toasterInstance = OverlayToaster.create({
  position: Position.BOTTOM,
  maxToasts: 5,
});

type ToastOptions = Omit<ToastProps, 'message' | 'intent'>;

export const AppToaster = {
  success: async (message: string, options?: ToastOptions) => {
    (await toasterInstance).show({
      message,
      intent: Intent.SUCCESS,
      icon: 'tick',
      timeout: 3000,
      ...options,
    });
  },

  error: async (message: string, options?: ToastOptions) => {
    (await toasterInstance).show({
      message,
      intent: Intent.DANGER,
      icon: 'error',
      timeout: 5000,
      ...options,
    });
  },

  info: async (message: string, options?: ToastOptions) => {
    (await toasterInstance).show({
      message,
      intent: Intent.PRIMARY,
      icon: 'info-sign',
      timeout: 3000,
      ...options,
    });
  },

  show: async (props: ToastProps) => {
    (await toasterInstance).show(props);
  },

  clear: async () => {
    (await toasterInstance).clear();
  },
};
