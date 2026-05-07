import { request } from '@/utils/request'

export function getWalletSummary() {
  return request({
    url: '/wallet',
    method: 'GET'
  })
}

export function bindBankCard(data) {
  return request({
    url: '/wallet/cards',
    method: 'POST',
    data
  })
}

export function rechargeWallet(data) {
  return request({
    url: '/wallet/recharge',
    method: 'POST',
    data
  })
}
