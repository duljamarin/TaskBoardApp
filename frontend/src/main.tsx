import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
import './index.css'

// Remove any stuck Vite error overlay from previous HMR errors
document.querySelectorAll('vite-error-overlay').forEach((el) => el.remove());

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)

