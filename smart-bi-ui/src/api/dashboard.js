import request from '@/utils/request'

// ========== 看板管理 ==========
export function listDashboard(query) {
  return request({
    url: '/api/dashboard/list',
    method: 'get',
    params: query
  })
}

export function getDashboard(id) {
  return request({
    url: '/api/dashboard/' + id,
    method: 'get'
  })
}

export function addDashboard(data) {
  return request({
    url: '/api/dashboard',
    method: 'post',
    data: data
  })
}

export function updateDashboard(data) {
  return request({
    url: '/api/dashboard',
    method: 'put',
    data: data
  })
}

export function delDashboard(ids) {
  return request({
    url: '/api/dashboard/' + ids,
    method: 'delete'
  })
}

export function refreshDashboard(id) {
  return request({
    url: '/api/dashboard/refresh/' + id,
    method: 'post'
  })
}

// 获取看板的卡片列表
export function getDashboardCards(id) {
  return request({
    url: '/api/dashboard/' + id + '/cards',
    method: 'get'
  })
}

// 执行图表卡 SQL 并返回图表数据（用于展示与刷新，数据不落库）
export function getChartCardData(cardId) {
  return request({
    url: '/api/dashboard/chart-card/' + cardId + '/data',
    method: 'get'
  })
}

// 保存看板布局配置
export function saveDashboardLayout(id, layoutData) {
  return request({
    url: '/api/dashboard/' + id + '/layout',
    method: 'post',
    data: layoutData
  })
}

// ========== 图表卡片管理 ==========
export function listChartCard(query) {
  return request({
    url: '/api/dashboard/card/list',
    method: 'get',
    params: query
  })
}

export function addChartCard(data) {
  return request({
    url: '/api/dashboard/card',
    method: 'post',
    data: data
  })
}

export function updateChartCard(data) {
  return request({
    url: '/api/dashboard/card',
    method: 'put',
    data: data
  })
}

export function delChartCard(ids) {
  return request({
    url: '/api/dashboard/card/' + ids,
    method: 'delete'
  })
}

// ========== 看板背景配置 ==========
export function getBackground(dashboardId) {
  return request({
    url: '/api/dashboard/' + dashboardId + '/background',
    method: 'get'
  })
}

export function updateBackground(dashboardId, data) {
  return request({
    url: '/api/dashboard/' + dashboardId + '/background',
    method: 'put',
    data: data
  })
}

// ========== 卡片组合 ==========
export function createGroup(dashboardId, data) {
  return request({
    url: '/api/dashboard/' + dashboardId + '/group',
    method: 'post',
    data: data
  })
}

export function ungroupCards(dashboardId, groupId) {
  return request({
    url: '/api/dashboard/' + dashboardId + '/group/' + groupId + '/ungroup',
    method: 'post'
  })
}

// ========== 数据源卡片配置 ==========
export function getDatasourceCardConfig(id) {
  return request({
    url: '/api/dashboard/datasource-card/' + id,
    method: 'get'
  })
}

export function addDatasourceCardConfig(data) {
  return request({
    url: '/api/dashboard/datasource-card',
    method: 'post',
    data: data
  })
}

export function updateDatasourceCardConfig(data) {
  return request({
    url: '/api/dashboard/datasource-card',
    method: 'put',
    data: data
  })
}

export function delDatasourceCardConfig(id) {
  return request({
    url: '/api/dashboard/datasource-card/' + id,
    method: 'delete'
  })
}

export function executeDatasourceCard(id) {
  return request({
    url: '/api/dashboard/datasource-card/' + id + '/execute',
    method: 'post'
  })
}

export function previewDatasourceQuery(data) {
  return request({
    url: '/api/dashboard/datasource-card/preview',
    method: 'post',
    data: data
  })
}

// ========== 报表生成（LLM 驱动大屏一键生成） ==========
export function generateReport(data) {
  return request({
    url: '/api/dashboard/report/generate',
    method: 'post',
    data: data,
    timeout: 120000
  })
}

/**
 * 报表生成 SSE 流式：通过回调实时接收 message / card / done / error，避免长耗时超时
 * @param {Object} data - { prompt, dashboardId }
 * @param {Object} callbacks - { onMessage(msg), onCard(payload), onDone(), onError(err) }
 * @returns {Promise<void>}
 */
export async function generateReportStream(data, callbacks = {}) {
  const baseURL = import.meta.env.VITE_APP_BASE_API || ''
  const url = baseURL + '/api/dashboard/report/generate/stream'
  const token = (await import('@/utils/auth')).getToken()
  const res = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: 'Bearer ' + token } : {})
    },
    body: JSON.stringify(data)
  })
  if (!res.ok) {
    const err = new Error(res.statusText || '请求失败')
    err.code = res.status
    if (callbacks.onError) callbacks.onError(err)
    return
  }
  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  let buf = ''
  let currentEvent = ''
  let currentData = ''
  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buf += decoder.decode(value, { stream: true })
    const lines = buf.split('\n')
    buf = lines.pop() || ''
    for (const line of lines) {
      if (line.startsWith('event:')) {
        currentEvent = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        currentData = line.slice(5).trim()
      } else if (line === '' && currentData !== '') {
        try {
          if (currentEvent === 'message') {
            if (callbacks.onMessage) callbacks.onMessage(currentData)
          } else if (currentEvent === 'card') {
            const payload = JSON.parse(currentData)
            if (callbacks.onCard) callbacks.onCard(payload)
          } else if (currentEvent === 'done') {
            if (callbacks.onDone) callbacks.onDone()
          } else if (currentEvent === 'error') {
            const err = JSON.parse(currentData)
            if (callbacks.onError) callbacks.onError(new Error(err.message || err.code))
          }
        } catch (e) {
          console.warn('SSE parse', e)
        }
        currentEvent = ''
        currentData = ''
      }
    }
  }
  if (currentData && currentEvent === 'done' && callbacks.onDone) callbacks.onDone()
}
