import { request } from '@/utils/request'

export function createFeedbackIssue(data) {
  return request({
    url: '/feedback/issues',
    method: 'POST',
    data
  })
}

export function getMyFeedbackIssues(bookingId) {
  return request({
    url: '/feedback/issues/my',
    method: 'GET',
    data: bookingId ? { bookingId } : {},
    contentType: 'query'
  })
}

export function chatAiAssistant(data) {
  return request({
    url: '/fault-report/chat',
    method: 'POST',
    data
  })
}

export function chatFaultReport(data) {
  return chatAiAssistant(data)
}
