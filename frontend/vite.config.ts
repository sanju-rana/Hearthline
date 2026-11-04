import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  // sockjs-client reads Node's `global`. The browser only has globalThis.
  define: {
    global: "globalThis"
  },
  server: {
    port: 5173,
    proxy: {
      "/api": "http://localhost:8080",
      "/ws": { target: "http://localhost:8080", ws: true },
      "/actuator": "http://localhost:8080"
    }
  }
});
