import { request } from '@/utils/request'

export function getPricingPlans() {
  return request({
    url: '/booking',
    method: 'GET'
  })
}

export function createBooking(scooterId, hiredPeriod) {
  return request({
    url: '/booking',
    method: 'POST',
    data: { scooterId, hiredPeriod },
    contentType: 'query'
  })
}

export function activateBooking(bookingId) {
  return request({
    url: '/booking/activate',
    method: 'POST',
    data: { bookingId },
    contentType: 'query'
  })
}
