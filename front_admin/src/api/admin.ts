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
  storeId: number | null
  rentalMode: string
  lockStatus: string
  location: string | null
  longitude: number | null
  latitude: number | null
  storeName: string | null
  storeAddress: string | null
  createdAt?: string
  updatedAt?: string
}

export interface ScooterUpsertPayload {
  id?: number
  scooterCode: string
  rentalMode?: string
  storeId?: number | null
  status?: string
  lockStatus?: string
  location?: string | null
  longitude?: number | null
  latitude?: number | null
}

export interface StoreDto {
  id?: number
  name: string
  address: string | null
  longitude: number | null
  latitude: number | null
  status: string
  totalInventory?: number
  currentAvailableInventory?: number
  bookableInventory?: number
  appointmentStart?: string | null
  appointmentEnd?: string | null
  createdAt?: string
  updatedAt?: string
}

export interface StoreUpsertPayload {
  name: string
  address?: string | null
  longitude: number | null
  latitude: number | null
  status?: string
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

export function resolveLocationFromCoordinates(longitude: number, latitude: number) {
  return request<ApiResponse<string>>({
    url: '/admin/scooter/resolve-location',
    method: 'get',
    params: { longitude, latitude }
  })
}

export function addScooter(data: ScooterUpsertPayload) {
  return request({
    url: '/admin/scooter/add',
    method: 'post',
    data
  })
}

export function updateScooter(data: ScooterUpsertPayload & { id: number }) {
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

export function listStores() {
  return request<ApiResponse<StoreDto[]>>({
    url: '/admin/stores',
    method: 'get'
  })
}

export function getStoreById(id: number) {
  return request<ApiResponse<StoreDto>>({
    url: `/admin/stores/${id}`,
    method: 'get'
  })
}

export function createStore(body: StoreUpsertPayload) {
  return request({
    url: '/admin/stores',
    method: 'post',
    data: body
  })
}

export function updateStore(id: number, body: StoreUpsertPayload) {
  return request({
    url: `/admin/stores/${id}`,
    method: 'put',
    data: body
  })
}

export function deleteStore(id: number) {
  return request({
    url: `/admin/stores/${id}`,
    method: 'delete'
  })
}

export interface PricingPlanDto {
  id?: number
  hirePeriod: string
  price: number
  updatedAt?: string
}

export interface AdminWeeklyRevenueBucket {
  hirePeriod: string
  orderCount: number
  totalRevenue: number
}

export interface AdminWeeklyRevenueSummary {
  windowStart: string
  windowEnd: string
  mostPopularHirePeriod: string | null
  buckets: AdminWeeklyRevenueBucket[]
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

export function getWeeklyRevenueSummary() {
  return request<ApiResponse<AdminWeeklyRevenueSummary>>({
    url: '/admin/revenue/weekly',
    method: 'get'
  })
}
