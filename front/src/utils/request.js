import { getToken, removeToken } from './auth'

const DEFAULT_WX_CLOUD_ENV_ID = 'prod-4g7i1ww2f71d4f7b'
const DEFAULT_WX_CLOUD_SERVICE = 'green-go'
const WX_CLOUD_ENV_ID = (import.meta.env.VITE_WX_CLOUD_ENV_ID || DEFAULT_WX_CLOUD_ENV_ID).trim()
const WX_CLOUD_SERVICE = (import.meta.env.VITE_WX_CLOUD_SERVICE || DEFAULT_WX_CLOUD_SERVICE).trim()
const MP_WEIXIN_REQUEST_MODE = (import.meta.env.VITE_MP_WEIXIN_REQUEST_MODE || 'cloud').trim().toLowerCase()
const BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/+$/, '')
const ABSOLUTE_HTTP_URL_RE = /^https?:\/\//i

function buildQueryString(params = {}) {
  return Object.keys(params)
    .filter((key) => params[key] !== undefined && params[key] !== null && params[key] !== '')
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&')
}

function buildRequestPayload(url, data, contentType, header) {
  const normalizedPath = url.startsWith('/') ? url : `/${url}`
  let finalPath = normalizedPath
  let finalData = data

  if (contentType === 'form') {
    header['Content-Type'] = 'application/x-www-form-urlencoded'
    finalData = buildQueryString(data)
  } else if (contentType === 'query') {
    const queryString = buildQueryString(data)
    finalPath += queryString ? `?${queryString}` : ''
    finalData = undefined
  } else {
    header['Content-Type'] = 'application/json'
  }

  return {
    finalPath,
    finalData
  }
}

function handleUnauthorized(reject) {
  removeToken()
  uni.showToast({ title: 'Please login first', icon: 'none' })
  uni.reLaunch({ url: '/pages/login/login' })
  reject(new Error('Unauthorized'))
}

function handleBusinessResponse(res, resolve, reject) {
  if (res.statusCode === 401) {
    handleUnauthorized(reject)
    return
  }

  const body = res.data
  if (body && body.code === 0) {
    resolve(body)
    return
  }

  uni.showToast({ title: body?.message || 'Request failed', icon: 'none' })
  reject(body || new Error('Request failed'))
}

function requestByCloudContainer(method, finalPath, finalData, header) {
  if (!WX_CLOUD_ENV_ID || !WX_CLOUD_SERVICE) {
    const error = new Error('WeChat cloud hosting config is incomplete')
    uni.showToast({ title: 'Cloud config missing', icon: 'none' })
    return Promise.reject(error)
  }

  return new Promise((resolve, reject) => {
    wx.cloud.callContainer({
      config: {
        env: WX_CLOUD_ENV_ID
      },
      path: finalPath,
      method,
      data: finalData,
      header: {
        ...header,
        'X-WX-SERVICE': WX_CLOUD_SERVICE
      },
      success(res) {
        handleBusinessResponse(res, resolve, reject)
      },
      fail(err) {
        uni.showToast({ title: 'Network error', icon: 'none' })
        reject(err)
      }
    })
  })
}

function requestByHttp(method, finalPath, finalData, header) {
  if (!BASE_URL) {
    const error = new Error('VITE_API_BASE_URL is not configured')
    uni.showToast({ title: 'API address not configured', icon: 'none' })
    return Promise.reject(error)
  }

  if (!ABSOLUTE_HTTP_URL_RE.test(BASE_URL)) {
    const error = new Error(`HTTP fallback needs an absolute API URL, got: ${BASE_URL}`)
    uni.showToast({ title: 'API URL must be absolute', icon: 'none' })
    return Promise.reject(error)
  }

  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + finalPath,
      method,
      data: finalData,
      header,
      success(res) {
        handleBusinessResponse(res, resolve, reject)
      },
      fail(err) {
        uni.showToast({ title: 'Network error', icon: 'none' })
        reject(err)
      }
    })
  })
}

export function request(options) {
  const { url, method = 'GET', data, contentType = 'json' } = options
  const header = {}
  const token = getToken()

  if (token) {
    header.Authorization = token
  }

  const { finalPath, finalData } = buildRequestPayload(url, data, contentType, header)

  // #ifdef MP-WEIXIN
  if (MP_WEIXIN_REQUEST_MODE === 'http') {
    return requestByHttp(method, finalPath, finalData, header)
  }
  return requestByCloudContainer(method, finalPath, finalData, header)
  // #endif

  return requestByHttp(method, finalPath, finalData, header)
}
