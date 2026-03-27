import request from '@/utils/request'

// 查询ETL任务列表
export function listEtlTask(query) {
  return request({
    url: '/api/etl/task/list',
    method: 'get',
    params: query
  })
}

// 查询ETL任务详细
export function getEtlTask(id) {
  return request({
    url: '/api/etl/task/' + id,
    method: 'get'
  })
}

// 新增ETL任务
export function addEtlTask(data) {
  return request({
    url: '/api/etl/task',
    method: 'post',
    data: data
  })
}

// 修改ETL任务
export function updateEtlTask(data) {
  return request({
    url: '/api/etl/task',
    method: 'put',
    data: data
  })
}

// 删除ETL任务
export function delEtlTask(ids) {
  return request({
    url: '/api/etl/task/' + ids,
    method: 'delete'
  })
}

// 手动触发ETL任务
export function triggerEtlTask(id) {
  return request({
    url: '/api/etl/task/trigger/' + id,
    method: 'post'
  })
}

// 暂停ETL任务
export function pauseEtlTask(id) {
  return request({
    url: '/api/etl/task/pause/' + id,
    method: 'post'
  })
}

// 恢复ETL任务
export function resumeEtlTask(id) {
  return request({
    url: '/api/etl/task/resume/' + id,
    method: 'post'
  })
}

// 查询ETL任务执行记录列表
export function listEtlTaskExecution(taskId) {
  return request({
    url: '/api/etl/task/execution/' + taskId,
    method: 'get'
  })
}

// 查询ETL任务执行记录列表（支持过滤）
export function listEtlTaskExecutions(query) {
  return request({
    url: '/api/etl/task/execution',
    method: 'get',
    params: query
  })
}

// 获取ETL任务监控数据
export function getEtlMonitorData(taskId, days) {
  return request({
    url: '/api/etl/task/monitor/data',
    method: 'get',
    params: { taskId, days }
  })
}

// 获取ETL任务状态概览
export function getEtlTaskStatusOverview() {
  return request({
    url: '/api/etl/task/monitor/overview',
    method: 'get'
  })
}

// 获取指定任务的执行状态
export function getEtlTaskStatus(taskId) {
  return request({
    url: '/api/etl/task/monitor/status/' + taskId,
    method: 'get'
  })
}

// 获取任务执行趋势数据
export function getEtlExecutionTrend(taskId, days) {
  return request({
    url: '/api/etl/task/monitor/trend',
    method: 'get',
    params: { taskId, days }
  })
}

// 获取实时运行中的任务列表
export function getRunningEtlTasks() {
  return request({
    url: '/api/etl/task/monitor/running',
    method: 'get'
  })
}