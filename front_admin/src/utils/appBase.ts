function normalizeBase(base: string | undefined): string {
  const value = (base || '/').trim()
  if (!value || value === '/') {
    return '/'
  }

  const withLeadingSlash = value.startsWith('/') ? value : `/${value}`
  return withLeadingSlash.endsWith('/') ? withLeadingSlash : `${withLeadingSlash}/`
}

export function getAdminUiBase(): string {
  const configuredBase = import.meta.env.VITE_ADMIN_ROUTER_BASE
  if (configuredBase) {
    return normalizeBase(configuredBase)
  }

  if (typeof window === 'undefined') {
    return '/'
  }

  return window.location.pathname.startsWith('/admin-ui') ? '/admin-ui/' : '/'
}

export function getAdminUiUrl(path: string): string {
  const adminUiBaseUrl = new URL(getAdminUiBase(), window.location.origin)
  const normalizedPath = `/${path.replace(/^\/+/, '')}`
  return `${adminUiBaseUrl.toString()}#${normalizedPath}`
}
