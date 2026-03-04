import { request } from '@/utils/request'

export function adminLogin(username, password) {
  return request({
    url: '/admin/login',
    method: 'POST',
    data: { username, password },
    contentType: 'form'
  })
}

export function addScooter(data) {
  return request({
    url: '/admin/scooter/add',
    method: 'POST',
    data,
    contentType: 'json'
  })
}

export function updateScooter(data) {
  return request({
    url: '/admin/scooter/update',
    method: 'POST',
    data,
    contentType: 'json'
  })
}

export function deleteScooter(id) {
  return request({
    url: '/admin/scooter/delete',
    method: 'DELETE',
    data: { id },
    contentType: 'query'
  })
}
