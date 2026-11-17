import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// En desarrollo, Vite reenvía /api, /media y /ws al backend (puerto 8080),
// así el navegador ve todo como un solo sitio. En producción se usa VITE_API_URL
// o el proxy del servidor (deploy/Caddyfile).
const backend = process.env.MEALUP_BACKEND ?? "http://localhost:8080";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": { target: backend, changeOrigin: true },
      "/media": { target: backend, changeOrigin: true },
      "/ws": { target: backend, changeOrigin: true, ws: true },
    },
  },
});
