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

export function updateBookingStatus(bookingId, status) {
  return request({
    url: '/booking/status',
    method: 'POST',
    data: { bookingId, status },
    contentType: 'query'
  })
}
