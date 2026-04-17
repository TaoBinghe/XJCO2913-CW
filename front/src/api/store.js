import { request } from '@/utils/request'

function sanitizeParams(params = {}) {
  return Object.keys(params).reduce((result, key) => {
    const value = params[key]
    if (value !== undefined && value !== null && value !== '') {
      result[key] = value
    }
    return result
  }, {})
}

export function getStoreList(params = {}) {
  return request({
    url: '/store/list',
    method: 'GET',
    data: sanitizeParams(params),
    contentType: 'query'
  })
}

export function getStoreDetail(storeId, params = {}) {
  return request({
    url: `/store/${storeId}`,
    method: 'GET',
    data: sanitizeParams(params),
    contentType: 'query'
  })
}
