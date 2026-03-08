import request from '@/utils/request'

export function getPricingPlans() {
  return request({
    url: '/booking',
    method: 'get'
  })
}
