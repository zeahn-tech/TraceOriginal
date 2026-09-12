import { defineConfig } from 'vitest/config'

export default defineConfig({
  test: {
    environment: 'node',
    include: ['firebase/emulator-tests/**/*.test.ts'],
    testTimeout: 20000,
    hookTimeout: 20000,
  },
})
