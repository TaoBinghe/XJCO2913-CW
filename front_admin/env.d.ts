/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_ADMIN_API_BASE_URL?: string
  readonly VITE_ADMIN_ROUTER_BASE?: string
  readonly VITE_ADMIN_AMAP_KEY?: string
  readonly VITE_ADMIN_AMAP_SECURITY_CODE?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
