import request from '@/utils/request'

// 执行自然语言查询（LLM 生成 SQL + 执行可能较慢，超时 2 分钟）
export function executeQuery(data) {
  return request({
    url: '/api/query/execute',
    method: 'post',
    data: data,
    timeout: 120000
  })
}

// 获取查询记录详情（含 generatedSql、executedSql、involvedTables，用于溯源）
export function getQueryRecord(id) {
  return request({
    url: '/api/query/record/' + id,
    method: 'get'
  })
}

// 生成查询结果总结
export function summarizeQuery(data) {
  return request({
    url: '/api/query/summarize',
    method: 'post',
    data: data
  })
}

// 下钻
export function drillQuery(data) {
  return request({
    url: '/api/query/drill',
    method: 'post',
    data: data
  })
}

// 获取查询历史
export function getQueryHistory(query) {
  return request({
    url: '/api/query/history',
    method: 'get',
    params: query
  })
}

// 获取筛选器建议
export function recommendFilters(data) {
  return request({
    url: '/api/query/filter/recommend',
    method: 'post',
    data: data
  })
}

// 推荐图表类型（LLM，可能较慢，与 executeQuery 一致 2 分钟）
export function recommendChartType(data) {
  return request({
    url: '/api/query/chart/recommend',
    method: 'post',
    data: data,
    timeout: 120000
  })
}

// 生成图表配置（LLM，可能较慢）
export function generateChartConfig(data) {
  return request({
    url: '/api/query/chart/config',
    method: 'post',
    data: data,
    timeout: 120000
  })
}

// 获取查询建议
export function getQuerySuggestions(params) {
  return request({
    url: '/api/query/suggest',
    method: 'get',
    params: params
  })
}

// 提交反馈
export function submitFeedback(data) {
  return request({
    url: '/api/query/feedback',
    method: 'post',
    data: data
  })
}

// 获取反馈列表
export function getFeedbackList(query) {
  return request({
    url: '/api/query/feedback/list',
    method: 'get',
    params: query
  })
}

// 审核反馈（QueryController 方式）
export function reviewFeedback(data) {
  return request({
    url: '/api/query/feedback/review',
    method: 'put',
    data: data
  })
}

// 反馈审核通过（FeedbackController，需 bi:feedback:approve）
export function approveFeedback(id, reviewComment) {
  return request({
    url: '/api/feedback/' + id + '/approve',
    method: 'put',
    data: reviewComment != null ? { reviewComment } : {}
  })
}

// 反馈审核驳回（FeedbackController，需 bi:feedback:approve）
export function rejectFeedback(id, reviewComment) {
  return request({
    url: '/api/feedback/' + id + '/reject',
    method: 'put',
    data: reviewComment != null ? { reviewComment } : {}
  })
}

// 获取异步任务状态
export function getTaskStatus(taskId) {
  return request({
    url: '/api/query/task/status/' + taskId,
    method: 'get'
  })
}

// 通过查询记录ID获取任务状态
export function getTaskStatusByQueryId(queryId) {
  return request({
    url: '/api/query/task/status/query/' + queryId,
    method: 'get'
  })
}

// 取消异步任务
export function cancelTask(taskId) {
  return request({
    url: '/api/query/task/cancel/' + taskId,
    method: 'post'
  })
}