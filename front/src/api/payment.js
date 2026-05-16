import { request } from '@/utils/request'

export function payBooking(data) {
  return request({
    url: '/payment',
    method: 'POST',
    data
  })
}
