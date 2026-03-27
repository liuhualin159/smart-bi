import request from '@/utils/request'

// 规则列表
export function listQualityRule(query) {
  return request({
    url: '/api/quality/rule/list',
    method: 'get',
    params: query
  })
}

// 规则详情
export function getQualityRule(id) {
  return request({
    url: '/api/quality/rule/' + id,
    method: 'get'
  })
}

// 新增规则
export function addQualityRule(data) {
  return request({
    url: '/api/quality/rule',
    method: 'post',
    data
  })
}

// 修改规则
export function updateQualityRule(data) {
  return request({
    url: '/api/quality/rule',
    method: 'put',
    data
  })
}

// 删除规则
export function delQualityRule(ids) {
  return request({
    url: '/api/quality/rule/' + ids,
    method: 'delete'
  })
}

// 执行单条规则
export function executeQualityRule(id) {
  return request({
    url: '/api/quality/rule/execute/' + id,
    method: 'post'
  })
}

// 规则测试（抽样）
export function testQualityRules(data) {
  return request({
    url: '/api/quality/rule/test',
    method: 'post',
    data
  })
}

// 计算质量评分
export function calculateQualityScore(data) {
  return request({
    url: '/api/quality/rule/score/calculate',
    method: 'post',
    data
  })
}

// 评分历史
export function getQualityScoreHistory(tableId) {
  return request({
    url: '/api/quality/rule/score/history',
    method: 'get',
    params: { tableId }
  })
}

// 触发告警检查
export function checkQualityAlert(data) {
  return request({
    url: '/api/quality/rule/alert/check',
    method: 'post',
    data
  })
}

// 生成报告
export function generateQualityReport(data) {
  return request({
    url: '/api/quality/report/generate',
    method: 'post',
    data: data || {}
  })
}

// 导出报告 Excel
export function exportQualityReport(data) {
  return request({
    url: '/api/quality/report/export/excel',
    method: 'post',
    data: data || {},
    responseType: 'blob'
  })
}
