import request from '@/utils/request'

// 订阅列表
export function listSubscription(query) {
  return request({
    url: '/api/subscription/list',
    method: 'get',
    params: query
  })
}

// 订阅详情
export function getSubscription(id) {
  return request({
    url: '/api/subscription/' + id,
    method: 'get'
  })
}

// 新增订阅
export function addSubscription(data) {
  return request({
    url: '/api/subscription',
    method: 'post',
    data
  })
}

// 修改订阅
export function updateSubscription(data) {
  return request({
    url: '/api/subscription',
    method: 'put',
    data
  })
}

// 删除订阅
export function delSubscription(ids) {
  return request({
    url: '/api/subscription/' + ids,
    method: 'delete'
  })
}

// 推送记录
export function getPushRecords(id, limit = 50) {
  return request({
    url: '/api/subscription/' + id + '/pushRecords',
    method: 'get',
    params: { limit }
  })
}
