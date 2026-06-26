import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Build output lands in Spring Boot static resources so the jar serves it.
export default defineConfig({
  plugins: [react()],
  base: '/react/',
  build: {
    outDir: '../src/main/resources/static/react',
    emptyOutDir: true,
  },
});
