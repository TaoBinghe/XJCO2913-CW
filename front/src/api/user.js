import { request } from '@/utils/request'

export function register(username, password) {
  return request({
    url: '/user/register',
    method: 'POST',
    data: { username, password },
    contentType: 'form'
  })
}

export function login(username, password) {
  return request({
    url: '/user/login',
    method: 'POST',
    data: { username, password },
    contentType: 'form'
  })
}

export function getMyOrders() {
  return request({
    url: '/user/my-orders',
    method: 'GET'
  })
}
