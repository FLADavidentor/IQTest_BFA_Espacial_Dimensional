import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Build output lands in Spring Boot static resources so the jar serves it.
// Fixed (un-hashed) filenames so the Thymeleaf shell can reference them directly.
export default defineConfig({
  plugins: [react()],
  base: '/react/',
  test: { environment: 'jsdom', globals: true },
  build: {
    outDir: '../src/main/resources/static/react',
    emptyOutDir: true,
    rollupOptions: {
      output: {
        entryFileNames: 'assets/bfa-subtest.js',
        chunkFileNames: 'assets/[name].js',
        assetFileNames: 'assets/bfa-subtest.[ext]',
      },
    },
  },
});
