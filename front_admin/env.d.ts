/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_ADMIN_API_BASE_URL?: string
  readonly VITE_ADMIN_ROUTER_BASE?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
