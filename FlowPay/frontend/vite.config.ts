// @lovable.dev/vite-tanstack-config already includes the following — do NOT add them manually
// or the app will break with duplicate plugins.
// Extra Vite options go inside the `vite` key.
import { defineConfig } from "@lovable.dev/vite-tanstack-config";

export default defineConfig({
  tanstackStart: {
    server: { entry: "server" },
  },
  vite: {
    server: {
      host: "localhost",
      port: 5173,
      proxy: {
        "/auth": {
          target: "http://localhost:8080",
          changeOrigin: true,
          secure: false,
        },
        "/api": {
          target: "http://localhost:8080",
          changeOrigin: true,
          secure: false,
        },
      },
    },
  },
});