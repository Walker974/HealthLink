export const env = {
  apiBaseUrl: (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, ''),
  keycloakUrl: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
  keycloakRealm: import.meta.env.VITE_KEYCLOAK_REALM || 'healthlink',
  keycloakClientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'healthlink-web',
  designPreview: import.meta.env.VITE_ENABLE_DESIGN_PREVIEW !== 'false',
} as const
