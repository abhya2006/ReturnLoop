import path from 'node:path';
import { build } from 'vite';
import react from '@vitejs/plugin-react';

const root = process.cwd();
await build({
  configFile: false,
  root,
  plugins: [react()],
  resolve: { alias: { '@': path.resolve(root, 'src') } },
  build: { outDir: 'dist', emptyOutDir: true },
});
