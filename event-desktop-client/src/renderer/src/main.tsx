import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';

const appMount = document.getElementById('app-mount')!;

createRoot(appMount).render(
  <StrictMode>
    <App />
  </StrictMode>
);
