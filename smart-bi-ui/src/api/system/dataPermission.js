import request from '@/utils/request'

// 查询数据权限列表
export function listDataPermission(query) {
  return request({
    url: '/system/dataPermission/list',
    method: 'get',
    params: query
  })
}

// 查询数据权限详细
export function getDataPermission(id) {
  return request({
    url: '/system/dataPermission/' + id,
    method: 'get'
  })
}

// 新增数据权限
export function addDataPermission(data) {
  return request({
    url: '/system/dataPermission',
    method: 'post',
    data: data
  })
}

// 修改数据权限
export function updateDataPermission(data) {
  return request({
    url: '/system/dataPermission',
    method: 'put',
    data: data
  })
}

// 删除数据权限
export function delDataPermission(ids) {
  return request({
    url: '/system/dataPermission/' + ids,
    method: 'delete'
  })
}

// 检查表权限
export function checkTablePermission(userId, tableName) {
  return request({
    url: '/system/dataPermission/checkTable/' + userId + '/' + tableName,
    method: 'get'
  })
}

// 检查字段权限
export function checkFieldPermission(userId, tableName, fieldName) {
  return request({
    url: '/system/dataPermission/checkField/' + userId + '/' + tableName + '/' + fieldName,
    method: 'get'
  })
}

// 获取行级过滤条件
export function getRowFilter(userId, tableName) {
  return request({
    url: '/system/dataPermission/rowFilter/' + userId + '/' + tableName,
    method: 'get'
  })
}
