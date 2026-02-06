import {
  Button,
  Checkbox,
  Classes,
  Drawer,
  DrawerSize,
  FormGroup,
  InputGroup,
  Intent,
} from '@blueprintjs/core';
import { useAppStore } from '@renderer/store/use-app-store';
import { AppToaster } from '@renderer/utils/app-toaster';
import { ServerInput } from '@shared-types/shared';
import React, { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';

type ServerFormData = {
  instantConnect: boolean;
} & ServerInput;

const AddServerDrawer: React.FC = (): React.ReactElement => {
  const {
    addServerDrawerActive,
    closeAddServerDrawer,
    setServers,
    openDefaultPasswordDialog,
    addActiveSession,
  } = useAppStore();

  const [showPassword, setShowPassword] = useState(false);
  const {
    reset,
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ServerFormData>({
    defaultValues: {
      name: '',
      url: '',
      username: '',
      password: '',
      instantConnect: false,
    },
  });

  const onSubmit = async ({ instantConnect, ...data }: ServerFormData): Promise<void> => {
    const serverId = await window.api.addServer(data);
    setServers(await window.api.getServers());
    await AppToaster.success('Server added successfully');
    handleClose();
    if (!instantConnect) {
      return;
    }
    const { success, error, hasDefaultPassword } = await window.api.connect(serverId);
    if (success) {
      if (hasDefaultPassword) {
        openDefaultPasswordDialog(serverId);
      }
      addActiveSession(serverId);
      await AppToaster.success('Connected to the server');
    }
    if (error) {
      await AppToaster.error(error);
    }
  };

  const lockButton = (
    <Button
      icon={showPassword ? 'unlock' : 'lock'}
      variant="minimal"
      onClick={() => setShowPassword(!showPassword)}
    />
  );

  const handleClose = (): void => {
    reset();
    closeAddServerDrawer();
  };

  return (
    <Drawer
      isOpen={addServerDrawerActive}
      onClose={handleClose}
      title="Add new proxy server"
      icon="add"
      size={DrawerSize.SMALL}>
      <form onSubmit={handleSubmit(onSubmit)}>
        <div className={Classes.DRAWER_BODY}>
          <div className={Classes.DIALOG_BODY}>
            <FormGroup
              label="Server name"
              labelFor="name-input"
              helperText={errors.name?.message}
              intent={errors.name ? Intent.DANGER : Intent.NONE}>
              <Controller
                name="name"
                control={control}
                rules={{ required: 'Server name is required' }}
                render={({ field }) => (
                  <InputGroup
                    {...field}
                    id="name-input"
                    intent={errors.name ? Intent.DANGER : Intent.NONE}
                    inputRef={field.ref}
                  />
                )}
              />
            </FormGroup>
            <FormGroup
              label="URL address or domain name"
              labelFor="url-input"
              helperText={errors.url?.message || 'http://localhost:8080'}
              intent={errors.url ? Intent.DANGER : Intent.NONE}>
              <Controller
                name="url"
                control={control}
                rules={{
                  required: 'URL is required',
                  pattern: {
                    value: /^(https?:\/\/)/,
                    message: 'URL must start with http:// or https://',
                  },
                }}
                render={({ field }) => (
                  <InputGroup
                    {...field}
                    id="url-input"
                    placeholder="http://192.168.1.1:8080"
                    intent={errors.url ? Intent.DANGER : Intent.NONE}
                    inputRef={field.ref}
                  />
                )}
              />
            </FormGroup>
            <FormGroup
              label="Username"
              labelFor="user-input"
              helperText={errors.username?.message}
              intent={errors.username ? Intent.DANGER : Intent.NONE}>
              <Controller
                name="username"
                control={control}
                rules={{ required: 'Username is required' }}
                render={({ field }) => (
                  <InputGroup
                    {...field}
                    id="user-input"
                    intent={errors.username ? Intent.DANGER : Intent.NONE}
                    inputRef={field.ref}
                  />
                )}
              />
            </FormGroup>
            <FormGroup
              label="Password"
              labelFor="pass-input"
              helperText={errors.password?.message}
              intent={errors.password ? Intent.DANGER : Intent.NONE}>
              <Controller
                name="password"
                control={control}
                rules={{ required: 'Password is required' }}
                render={({ field }) => (
                  <InputGroup
                    {...field}
                    id="pass-input"
                    type={showPassword ? 'text' : 'password'}
                    rightElement={lockButton}
                    intent={errors.password ? Intent.DANGER : Intent.NONE}
                    inputRef={field.ref}
                  />
                )}
              />
            </FormGroup>
            <div>
              <Controller
                name="instantConnect"
                control={control}
                render={({ field: { value, ref, ...fieldProps } }) => (
                  <Checkbox
                    {...fieldProps}
                    checked={value}
                    inputRef={ref}
                    label="Instant connect after add"
                  />
                )}
              />
            </div>
          </div>
        </div>
        <div className={Classes.DRAWER_FOOTER}>
          <Button type="submit" loading={isSubmitting} text="Add server" />
        </div>
      </form>
    </Drawer>
  );
};

export default AddServerDrawer;
