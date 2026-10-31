import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [
      react(),
      tailwindcss(),
      VitePWA({
        registerType: 'prompt',
        injectRegister: false,
        includeAssets: ['favicon.svg', 'icon-maskable.svg', 'icon-180.png', 'icon-192.png', 'icon-512.png'],
        manifest: {
          id: '/',
          name: 'HealthLink Morocco',
          short_name: 'HealthLink',
          description: 'Public hospital imaging availability, made clear.',
          start_url: '/',
          scope: '/',
          display: 'standalone',
          background_color: '#f5f7f6',
          theme_color: '#0e6b5c',
          lang: 'en',
          categories: ['health', 'medical'],
          icons: [
            { src: '/icon-192.png', sizes: '192x192', type: 'image/png', purpose: 'any' },
            { src: '/icon-512.png', sizes: '512x512', type: 'image/png', purpose: 'any maskable' },
            { src: '/favicon.svg', sizes: 'any', type: 'image/svg+xml', purpose: 'any' },
            { src: '/icon-maskable.svg', sizes: 'any', type: 'image/svg+xml', purpose: 'maskable' },
          ],
        },
        workbox: {
          globPatterns: ['**/*.{js,css,html,svg,png,woff,woff2}'],
          maximumFileSizeToCacheInBytes: 3_000_000,
          navigateFallbackDenylist: [/^\/api(?:\/|$)/, /^\/realms\//, /^\/swagger/],
          // Authenticated API responses and external map tiles are never cached here.
          runtimeCaching: [],
          cleanupOutdatedCaches: true,
        },
      }),
    ],
    resolve: { alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) } },
    server: {
      port: 5173,
      strictPort: true,
      host: "0.0.0.0",
      proxy: { '/api': { target: env.API_PROXY_TARGET || 'http://127.0.0.1:8080', changeOrigin: true } },
    },
    preview: {
      port: 4173,
      strictPort: true,
      proxy: { '/api': { target: env.API_PROXY_TARGET || 'http://127.0.0.1:8080', changeOrigin: true } },
    },
    build: { target: 'es2022', chunkSizeWarningLimit: 800 },
  }
})
