import { Alert, Classes, Intent } from '@blueprintjs/core';
import { BlueprintIcons_16Id } from '@blueprintjs/icons/lib/esnext/generated/16px/blueprint-icons-16';
import React from 'react';

type Props = {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  loading?: boolean;
  title?: string;
  children: React.ReactNode;
  confirmButtonText?: string;
  cancelButtonText?: string;
  intent?: Intent;
  icon?: BlueprintIcons_16Id;
};

const ConfirmAlert: React.FC<Props> = ({
  isOpen,
  onClose,
  onConfirm,
  loading = false,
  children,
  confirmButtonText = 'Confirm',
  cancelButtonText = 'Cancel',
  intent = Intent.NONE,
  icon,
}): React.ReactElement => {
  return (
    <Alert
      className={Classes.ALERT}
      isOpen={isOpen}
      loading={loading}
      icon={icon}
      intent={intent}
      confirmButtonText={confirmButtonText}
      cancelButtonText={cancelButtonText}
      onConfirm={onConfirm}
      onCancel={() => {
        if (!loading) {
          onClose();
        }
      }}
      canEscapeKeyCancel={!loading}
      canOutsideClickCancel={!loading}>
      <p>{children}</p>
    </Alert>
  );
};

export default ConfirmAlert;
