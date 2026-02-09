import axios from 'axios';

export const extractErrorMessage = (err: unknown): string => {
  if (axios.isAxiosError(err)) {
    let msg = err.message;
    if (err.response) {
      msg += ` (status: ${err.response.status})`;
    }
    return msg || `Unknown error: ${err.code}`;
  }
  if (err instanceof Error) {
    return err.message;
  }
  return String(err);
};
