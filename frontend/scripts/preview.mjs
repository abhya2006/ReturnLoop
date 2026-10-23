import { preview } from 'vite';

const server = await preview({
  configFile: false,
  root: process.cwd(),
  preview: {
    host: '0.0.0.0',
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': { target: process.env.VITE_API_PROXY || 'http://localhost:8080', changeOrigin: true },
      '/actuator': { target: process.env.VITE_API_PROXY || 'http://localhost:8080', changeOrigin: true },
    },
  },
});
server.printUrls();
