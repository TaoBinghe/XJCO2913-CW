import { request } from '@/utils/request'

export function getReservationPricingPlans() {
  return request({
    url: '/booking',
    method: 'GET'
  })
}

export function createStoreBooking({ storeId, appointmentStart, hiredPeriod }) {
  return request({
    url: '/booking',
    method: 'POST',
    data: { storeId, appointmentStart, hiredPeriod }
  })
}

export function startScanRide(scooterCode) {
  return request({
    url: '/booking/scan/start',
    method: 'POST',
    data: { scooterCode }
  })
}

export function cancelStoreBooking(bookingId) {
  return request({
    url: `/booking/${bookingId}/cancel`,
    method: 'POST'
  })
}

export function getPickupScooters(bookingId) {
  return request({
    url: `/booking/${bookingId}/pickup-scooters`,
    method: 'GET'
  })
}

export function pickupStoreBooking(bookingId, scooterId) {
  return request({
    url: `/booking/${bookingId}/pickup`,
    method: 'POST',
    data: { scooterId }
  })
}

export function lockBooking(bookingId) {
  return request({
    url: `/booking/${bookingId}/lock`,
    method: 'POST'
  })
}

export function unlockBooking(bookingId) {
  return request({
    url: `/booking/${bookingId}/unlock`,
    method: 'POST'
  })
}

export function returnStoreBooking(bookingId) {
  return request({
    url: `/booking/${bookingId}/return`,
    method: 'POST'
  })
}

export function returnScanRide(bookingId, longitude, latitude) {
  return request({
    url: `/booking/scan/${bookingId}/return`,
    method: 'POST',
    data: { longitude, latitude }
  })
}
