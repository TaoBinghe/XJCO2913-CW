import { request } from '@/utils/request'

export function pay(bookingId) {
  return request({
    url: '/payment',
    method: 'POST',
    data: { bookingId },
    contentType: 'query'
  })
}
