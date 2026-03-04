const TOKEN_KEY = 'token'
const ROLE_KEY = 'userRole'
const USERNAME_KEY = 'username'

export function getToken() {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

export function setToken(token) {
  uni.setStorageSync(TOKEN_KEY, token)
}

export function removeToken() {
  uni.removeStorageSync(TOKEN_KEY)
}

export function getUserRole() {
  return uni.getStorageSync(ROLE_KEY) || ''
}

export function setUserRole(role) {
  uni.setStorageSync(ROLE_KEY, role)
}

export function getUsername() {
  return uni.getStorageSync(USERNAME_KEY) || ''
}

export function setUsername(name) {
  uni.setStorageSync(USERNAME_KEY, name)
}

export function clearAll() {
  removeToken()
  uni.removeStorageSync(ROLE_KEY)
  uni.removeStorageSync(USERNAME_KEY)
}
