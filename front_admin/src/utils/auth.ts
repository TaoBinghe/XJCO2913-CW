const TOKEN_KEY = 'admin_token'
const USERNAME_KEY = 'admin_username'

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

export function getUsername(): string {
  return localStorage.getItem(USERNAME_KEY) || ''
}

export function setUsername(name: string): void {
  localStorage.setItem(USERNAME_KEY, name)
}

export function clearAll(): void {
  removeToken()
  localStorage.removeItem(USERNAME_KEY)
}
