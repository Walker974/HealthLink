import Keycloak from 'keycloak-js'
import { env } from '@/lib/env'

// Token storage belongs exclusively to the adapter's in-memory session.
let client: Keycloak | undefined
let initialization: Promise<boolean> | undefined

export function getKeycloak() {
  client ??= new Keycloak({ url: env.keycloakUrl, realm: env.keycloakRealm, clientId: env.keycloakClientId })
  return client
}

export function initializeKeycloak() {
  if (!initialization) {
    initialization = getKeycloak().init({
      pkceMethod: 'S256', checkLoginIframe: false,
      // Public search does not force sign-in. init still processes an OIDC callback.
    }).catch(error => { initialization = undefined; client = undefined; throw error })
  }
  return initialization
}
