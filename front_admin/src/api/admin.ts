import request from '@/utils/request'
import type { ApiResponse } from '@/utils/request'

export interface AdminUserDto {
  id: number
  username: string
  email: string | null
  role: string
  status: number
  createdAt: string
  updatedAt: string
}

export interface ScooterDto {
  id?: number
  scooterCode: string
  status: string
  location: string
}

export function adminLogin(username: string, password: string) {
  return request<ApiResponse<string>>({
    url: '/admin/login',
    method: 'post',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    data: `username=${encodeURIComponent(username)}&password=${encodeURIComponent(password)}`
  })
}

export function listUsers() {
  return request<ApiResponse<AdminUserDto[]>>({
    url: '/admin/user/list',
    method: 'get'
  })
}

export function listScooters() {
  return request<ApiResponse<ScooterDto[]>>({
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

// ----- Admin Pricing Plans CRUD -----

export interface PricingPlanDto {
  id?: number
  hirePeriod: string
  price: number
  updatedAt?: string
}

export function getPricingPlanList() {
  return request<ApiResponse<PricingPlanDto[]>>({
    url: '/admin/pricing-plans',
    method: 'get'
  })
}

export function getPricingPlanById(id: number) {
  return request<ApiResponse<PricingPlanDto>>({
    url: `/admin/pricing-plans/${id}`,
    method: 'get'
  })
}

export function createPricingPlan(body: { hirePeriod: string; price: number }) {
  return request({
    url: '/admin/pricing-plans',
    method: 'post',
    data: body
  })
}

export function updatePricingPlan(id: number, body: { hirePeriod?: string; price?: number }) {
  return request({
    url: `/admin/pricing-plans/${id}`,
    method: 'put',
    data: body
  })
}

export function deletePricingPlan(id: number) {
  return request({
    url: `/admin/pricing-plans/${id}`,
    method: 'delete'
  })
}
