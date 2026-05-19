import { defineConfig } from "vite";

export default defineConfig({
  server: {
    proxy: {
      "/product-api": {
        target: "http://localhost:8080",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/product-api/, "")
      },
      "/order-api": {
        target: "http://localhost:8083",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/order-api/, "")
      }
    }
  }
});
