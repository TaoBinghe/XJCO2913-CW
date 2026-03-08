import request from '@/utils/request'

export function adminLogin(username: string, password: string) {
  return request({
    url: '/admin/login',
    method: 'post',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    data: `username=${encodeURIComponent(username)}&password=${encodeURIComponent(password)}`
  })
}

export function listUsers() {
  return request({
    url: '/admin/user/list',
    method: 'get'
  })
}

export function listScooters() {
  return request({
    url: '/admin/scooter/list',
    method: 'get'
  })
}

export function addScooter(data: { scooterCode: string; status?: string; location?: string }) {
  return request({
    url: '/admin/scooter/add',
    method: 'post',
    data
  })
}

export function updateScooter(data: { id: number; scooterCode?: string; status?: string; location?: string }) {
  return request({
    url: '/admin/scooter/update',
    method: 'post',
    data
  })
}

export function deleteScooter(id: number) {
  return request({
    url: '/admin/scooter/delete',
    method: 'delete',
    params: { id }
  })
}
