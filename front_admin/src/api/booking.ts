import request from '@/utils/request'
import type { ApiResponse } from '@/utils/request'
import type { PricingPlanDto } from './admin'

export function getPricingPlans() {
  return request<ApiResponse<PricingPlanDto[]>>({
    url: '/booking',
    method: 'get'
  })
}
