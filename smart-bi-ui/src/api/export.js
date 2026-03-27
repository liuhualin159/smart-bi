import request from '@/utils/request'
import { download, downloadGet } from '@/utils/request'
import { saveAs } from 'file-saver'
import { ElLoading } from 'element-plus'

// 导出图表为PNG（后端为 GET 接口）
export function exportChartPng(cardId) {
  return downloadGet('/api/export/chart/png/' + cardId, 'chart_' + cardId + '.png')
}

// 导出图表为PDF（后端为 GET 接口）
export function exportChartPdf(cardId) {
  return downloadGet('/api/export/chart/pdf/' + cardId, 'chart_' + cardId + '.pdf')
}

// 导出数据为Excel（GET，兼容旧接口）
export function exportDataExcel(params) {
  return download('/api/export/data/excel', params, 'data_export.xlsx')
}

/**
 * 按格式导出数据（CSV、JSON、Parquet、Excel），支持脱敏
 * 使用 POST JSON 请求体
 * @param {Object} params - { queryId, sql?, format: 'excel'|'csv'|'json'|'parquet', maxRows?, applyDesensitization? }
 */
export function exportData(params) {
  const format = params.format || 'excel'
  const ext = { excel: 'xlsx', csv: 'csv', json: 'json', parquet: 'parquet' }[format] || 'xlsx'
  const loading = ElLoading.service({ text: '正在导出，请稍候', background: 'rgba(0,0,0,0.7)' })
  return request.post('/api/export/data', params, {
    responseType: 'blob',
    headers: { 'Content-Type': 'application/json' }
  }).then((data) => {
    const blob = new Blob([data])
    saveAs(blob, `data_export.${ext}`)
    return data
  }).catch((err) => {
    console.error('导出失败', err)
    throw err
  }).finally(() => {
    loading.close()
  })
}
