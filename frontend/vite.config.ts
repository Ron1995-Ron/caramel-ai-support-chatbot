import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// The browser only ever talks to the backend through this proxy. The Google AI Studio key is
// read by the backend alone and must never appear in any frontend file or build output.
const apiTarget = process.env.VITE_API_TARGET ?? 'http://localhost:8083'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5176,
    strictPort: true,
    proxy: {
      '/api': { target: apiTarget, changeOrigin: true },
    },
  },
})
