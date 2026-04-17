const DURATION_UNIT_ORDER = ['MINUTE', 'HOUR', 'DAY', 'WEEK', 'MONTH']
const HIRE_PERIOD_PATTERN = /^(MINUTE|HOUR|DAY|WEEK|MONTH)_(\d+)$/i
const OPEN_BOOKING_STATUSES = ['RESERVED', 'IN_PROGRESS', 'OVERDUE']
const HISTORY_BOOKING_STATUSES = ['COMPLETED', 'CANCELLED', 'NO_SHOW_CANCELLED']
const STATUS_TONE_MAP = {
  RESERVED: 'reserved',
  IN_PROGRESS: 'active',
  OVERDUE: 'overdue',
  COMPLETED: 'completed',
  CANCELLED: 'cancelled',
  NO_SHOW_CANCELLED: 'cancelled'
}
const RENTAL_TYPE_LABELS = {
  STORE_PICKUP: 'Store Pickup',
  SCAN_RIDE: 'Scan Ride'
}

const DURATION_UNIT_CONFIG = {
  MINUTE: { singular: 'Minute', plural: 'Minutes', short: 'M', sortMinutes: 1 },
  HOUR: { singular: 'Hour', plural: 'Hours', short: 'H', sortMinutes: 60 },
  DAY: { singular: 'Day', plural: 'Days', short: 'D', sortMinutes: 1440 },
  WEEK: { singular: 'Week', plural: 'Weeks', short: 'W', sortMinutes: 10080 },
  MONTH: { singular: 'Month', plural: 'Months', short: 'MO', sortMinutes: 43200 }
}

export function parseHirePeriod(period) {
  const normalized = String(period || '').trim().toUpperCase()
  const match = normalized.match(HIRE_PERIOD_PATTERN)
  if (!match) return null

  const value = Number(match[2])
  if (!Number.isInteger(value) || value <= 0) return null

  const unit = match[1]
  return {
    unit,
    value,
    code: `${unit}_${value}`,
    sortWeight: value * DURATION_UNIT_CONFIG[unit].sortMinutes
  }
}

export function formatPeriod(period) {
  const parsed = parseHirePeriod(period)
  if (!parsed) return period || '-'

  const config = DURATION_UNIT_CONFIG[parsed.unit]
  return `${parsed.value} ${parsed.value === 1 ? config.singular : config.plural}`
}

export function formatPeriodBadge(period) {
  const parsed = parseHirePeriod(period)
  if (!parsed) return period || '-'

  return `${parsed.value}${DURATION_UNIT_CONFIG[parsed.unit].short}`
}

export function formatCurrency(value) {
  const amount = Number(value || 0)
  return `\u00A3${amount.toFixed(2)}`
}

export function formatTime(timeStr) {
  if (!timeStr) return '-'
  return String(timeStr).replace('T', ' ').slice(0, 16)
}

export function isOpenBooking(status) {
  return OPEN_BOOKING_STATUSES.includes(String(status || '').toUpperCase())
}

export function isHistoryBooking(status) {
  return HISTORY_BOOKING_STATUSES.includes(String(status || '').toUpperCase())
}

export function getBookingTone(status) {
  const normalizedStatus = String(status || '').toUpperCase()
  return STATUS_TONE_MAP[normalizedStatus] || 'reserved'
}

export function getRentalTypeLabel(rentalType) {
  const normalizedType = String(rentalType || '').toUpperCase()
  return RENTAL_TYPE_LABELS[normalizedType] || (rentalType || '-')
}

export function sortBookings(bookings = []) {
  return [...bookings].sort((left, right) => {
    const rightTime = Date.parse(right?.createdAt || right?.updatedAt || '') || 0
    const leftTime = Date.parse(left?.createdAt || left?.updatedAt || '') || 0
    if (rightTime !== leftTime) {
      return rightTime - leftTime
    }
    return Number(right?.id || 0) - Number(left?.id || 0)
  })
}

export function sortPricingPlans(plans = []) {
  return [...plans].sort((left, right) => {
    const leftParsed = parseHirePeriod(left.hirePeriod)
    const rightParsed = parseHirePeriod(right.hirePeriod)

    if (leftParsed && rightParsed) {
      if (leftParsed.sortWeight !== rightParsed.sortWeight) {
        return leftParsed.sortWeight - rightParsed.sortWeight
      }

      const leftUnitOrder = DURATION_UNIT_ORDER.indexOf(leftParsed.unit)
      const rightUnitOrder = DURATION_UNIT_ORDER.indexOf(rightParsed.unit)
      if (leftUnitOrder !== rightUnitOrder) {
        return leftUnitOrder - rightUnitOrder
      }

      return leftParsed.value - rightParsed.value
    }

    if (leftParsed) return -1
    if (rightParsed) return 1
    return String(left.hirePeriod || '').localeCompare(String(right.hirePeriod || ''))
  })
}

export function sortScooters(scooters = []) {
  return [...scooters].sort((left, right) => {
    if (left.status !== right.status) {
      return left.status === 'AVAILABLE' ? -1 : 1
    }

    const leftCode = left.scooterCode || ''
    const rightCode = right.scooterCode || ''
    return leftCode.localeCompare(rightCode)
  })
}

export function buildEntityMap(items = [], key = 'id') {
  return items.reduce((map, item) => {
    if (item && item[key] != null) {
      map[String(item[key])] = item
    }
    return map
  }, {})
}

function getBookingTitle(booking) {
  if (booking.rentalType === 'STORE_PICKUP') {
    return booking.storeName || 'Store Reservation'
  }
  return booking.scooterCode || 'Scan Ride'
}

function getBookingLocation(booking) {
  if (booking.rentalType === 'STORE_PICKUP') {
    return booking.storeAddress || booking.pickupLocation || 'Store address unavailable'
  }
  return booking.returnLocation || booking.pickupLocation || 'Map location unavailable'
}

function getBookingTimelineLabel(booking) {
  if (booking.rentalType === 'STORE_PICKUP') {
    return booking.status === 'RESERVED'
      ? formatTime(booking.startTime)
      : formatTime(booking.returnTime || booking.endTime)
  }
  return formatTime(booking.returnTime || booking.startTime)
}

export function buildBookingViewModel(booking) {
  const normalizedStatus = String(booking?.status || '').toUpperCase()

  return {
    ...booking,
    status: normalizedStatus,
    rentalTypeLabel: getRentalTypeLabel(booking?.rentalType),
    statusTone: getBookingTone(normalizedStatus),
    displayTitle: getBookingTitle(booking || {}),
    displayLocation: getBookingLocation(booking || {}),
    timelineLabel: getBookingTimelineLabel(booking || {}),
    hirePeriodLabel: formatPeriod(booking?.hirePeriod),
    hirePeriodBadge: formatPeriodBadge(booking?.hirePeriod),
    totalCostValue: Number(booking?.totalCost || 0),
    overdueCostValue: Number(booking?.overdueCost || 0)
  }
}
