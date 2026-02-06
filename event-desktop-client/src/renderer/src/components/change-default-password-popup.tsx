import { Button, Classes, Dialog, FormGroup, InputGroup, Intent } from '@blueprintjs/core';
import { useAppStore } from '@renderer/store/use-app-store';
import { AppToaster } from '@renderer/utils/app-toaster';
import React, { useMemo, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';

type FormData = {
  newPassword: string;
  confirmPassword: string;
};

const ChangeDefaultPasswordPopup: React.FC = (): React.ReactElement => {
  const { updateDefaultPasswordServerId, closeDefaultPasswordDialog } = useAppStore();

  const isOpen = useMemo(() => !!updateDefaultPasswordServerId, [updateDefaultPasswordServerId]);
  const [showPassword, setShowPassword] = useState(false);

  const {
    reset,
    control,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    defaultValues: {
      newPassword: '',
      confirmPassword: '',
    },
  });

  const onSubmit = async (data: FormData): Promise<void> => {
    if (!updateDefaultPasswordServerId) {
      return;
    }
    const { success, error } = await window.api.updateDefaultPassword(
      updateDefaultPasswordServerId,
      data.newPassword
    );
    if (success) {
      await AppToaster.success('Password updated');
      handleClose();
    }
    if (error) {
      await AppToaster.error(error);
    }
  };

  const handleClose = (): void => {
    reset();
    setShowPassword(false);
    closeDefaultPasswordDialog();
  };

  const lockButton = (
    <Button
      icon={showPassword ? 'unlock' : 'lock'}
      variant="minimal"
      onClick={() => setShowPassword(!showPassword)}
    />
  );

  return (
    <Dialog isOpen={isOpen} onClose={handleClose} title="Change default password" icon="key">
      <form onSubmit={handleSubmit(onSubmit)}>
        <div className={Classes.DIALOG_BODY}>
          <FormGroup
            label="New password"
            labelFor="new-pass"
            helperText={errors.newPassword?.message}
            intent={errors.newPassword ? Intent.DANGER : Intent.NONE}>
            <Controller
              name="newPassword"
              control={control}
              rules={{
                required: 'Password is required',
                minLength: { value: 4, message: 'Password must have at least 4 characters' },
              }}
              render={({ field }) => (
                <InputGroup
                  {...field}
                  id="new-pass"
                  type={showPassword ? 'text' : 'password'}
                  rightElement={lockButton}
                  intent={errors.newPassword ? Intent.DANGER : Intent.NONE}
                  inputRef={field.ref}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label="Confirm new password"
            labelFor="confirm-pass"
            helperText={errors.confirmPassword?.message}
            intent={errors.confirmPassword ? Intent.DANGER : Intent.NONE}>
            <Controller
              name="confirmPassword"
              control={control}
              rules={{
                required: 'Confirmed password is required',
                validate: val => watch('newPassword') === val || 'Passwords are not the same',
              }}
              render={({ field }) => (
                <InputGroup
                  {...field}
                  id="confirm-pass"
                  type={showPassword ? 'text' : 'password'}
                  intent={errors.confirmPassword ? Intent.DANGER : Intent.NONE}
                  inputRef={field.ref}
                />
              )}
            />
          </FormGroup>
        </div>
        <div className={Classes.DIALOG_FOOTER}>
          <div className={Classes.DIALOG_FOOTER_ACTIONS}>
            <Button onClick={handleClose} text="Cancel" variant="minimal" />
            <Button type="submit" text="Change password" loading={isSubmitting} />
          </div>
        </div>
      </form>
    </Dialog>
  );
};

export default ChangeDefaultPasswordPopup;
