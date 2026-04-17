import { request } from '@/utils/request'

export function getScooterList() {
  return request({
    url: '/scooter/list',
    method: 'GET'
  })
}

export function getScooterRoute(scooterId, fromLongitude, fromLatitude) {
  return request({
    url: '/scooter/route',
    method: 'GET',
    data: {
      scooterId,
      fromLongitude,
      fromLatitude
    },
    contentType: 'query'
  })
}
