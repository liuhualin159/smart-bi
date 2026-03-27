import request from '@/utils/request'
import { parseStrEmpty } from "@/utils/ruoyi";

// 查询数据源列表
export function listDataSource(query) {
  return request({
    url: '/api/datasource/list',
    method: 'get',
    params: query
  })
}

// 查询数据源详细
export function getDataSource(id) {
  return request({
    url: '/api/datasource/' + parseStrEmpty(id),
    method: 'get'
  })
}

// 新增数据源
export function addDataSource(data) {
  return request({
    url: '/api/datasource',
    method: 'post',
    data: data
  })
}

// 修改数据源
export function updateDataSource(data) {
  return request({
    url: '/api/datasource',
    method: 'put',
    data: data
  })
}

// 删除数据源
export function delDataSource(id) {
  return request({
    url: '/api/datasource/' + id,
    method: 'delete'
  })
}

// 测试数据源连接
export function testDataSourceConnection(data) {
  return request({
    url: '/api/datasource/test',
    method: 'post',
    data: data
  })
}

// 查询数据源的表列表
export function getTableList(dataSourceId) {
  return request({
    url: '/api/datasource/' + dataSourceId + '/tables',
    method: 'get'
  })
}

// 查询指定表的字段列表
export function getColumnList(dataSourceId, tableName) {
  return request({
    url: '/api/datasource/' + dataSourceId + '/tables/' + tableName + '/columns',
    method: 'get'
  })
}

// 查询本地数据库的表列表
export function getLocalTableList() {
  return request({
    url: '/api/datasource/local/tables',
    method: 'get'
  })
}

// 查询本地数据库指定表的字段列表（元数据管理用）
export function getLocalColumnList(tableName) {
  return request({
    url: '/api/datasource/local/tables/' + encodeURIComponent(tableName) + '/columns',
    method: 'get'
  })
}

// 根据源表结构自动创建目标表
export function createTargetTable(data) {
  return request({
    url: '/api/datasource/create-target-table',
    method: 'post',
    data: data
  })
}
