import request from '@/utils/request'

/**
 * 预览表数据
 * @param {number} tableId 表元数据ID
 * @param {number} [limit=100] 最大行数
 */
export function previewTable(tableId, limit = 100) {
  return request({
    url: '/api/explore/table/preview',
    method: 'get',
    params: { tableId, limit }
  })
}
