const TOKEN_KEY = 'token'
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

export function getUsername() {
  return uni.getStorageSync(USERNAME_KEY) || ''
}

export function setUsername(name) {
  uni.setStorageSync(USERNAME_KEY, name)
}

export function clearAll() {
  removeToken()
  uni.removeStorageSync('userRole')
  uni.removeStorageSync(USERNAME_KEY)
}
