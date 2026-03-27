import request from '@/utils/request'

// ========== 业务域管理 ==========
export function listBusinessDomain(query) {
  return request({
    url: '/api/metadata/domain/list',
    method: 'get',
    params: query
  })
}

export function getBusinessDomain(id) {
  return request({
    url: '/api/metadata/domain/' + id,
    method: 'get'
  })
}

export function addBusinessDomain(data) {
  return request({
    url: '/api/metadata/domain',
    method: 'post',
    data: data
  })
}

export function updateBusinessDomain(data) {
  return request({
    url: '/api/metadata/domain',
    method: 'put',
    data: data
  })
}

export function delBusinessDomain(ids) {
  return request({
    url: '/api/metadata/domain/' + ids,
    method: 'delete'
  })
}

// ========== 表元数据管理 ==========
export function listTableMetadata(query) {
  return request({
    url: '/api/metadata/table/list',
    method: 'get',
    params: query
  })
}

export function getTableProblemConfig() {
  return request({
    url: '/api/metadata/table/problem-config',
    method: 'get'
  })
}

export function updateTableProblemConfig(data) {
  return request({
    url: '/api/metadata/table/problem-config',
    method: 'put',
    data
  })
}

export function getTableMetadata(id) {
  return request({
    url: '/api/metadata/table/' + id,
    method: 'get'
  })
}

export function addTableMetadata(data) {
  return request({
    url: '/api/metadata/table',
    method: 'post',
    data: data
  })
}

export function updateTableMetadata(data) {
  return request({
    url: '/api/metadata/table',
    method: 'put',
    data: data
  })
}

/** 行内编辑单条表（用途/可见性/粒度），带乐观锁 */
export function updateTableMetadataById(id, data) {
  return request({
    url: '/api/metadata/table/' + id,
    method: 'put',
    data: data
  })
}

/** 批量更新表的用途与可见性 */
export function batchUpdateTableMetadata(data) {
  return request({
    url: '/api/metadata/table/batch',
    method: 'put',
    data: data
  })
}

export function delTableMetadata(ids) {
  return request({
    url: '/api/metadata/table/' + ids,
    method: 'delete'
  })
}

// ========== 字段元数据管理 ==========
export function listFieldMetadata(query) {
  return request({
    url: '/api/metadata/field/list',
    method: 'get',
    params: query
  })
}

export function listFieldMetadataByTable(tableId) {
  return request({
    url: '/api/metadata/field/table/' + tableId,
    method: 'get'
  })
}

export function updateFieldMetadataById(id, data) {
  return request({
    url: '/api/metadata/field/' + id,
    method: 'put',
    data: data
  })
}

export function addFieldAlias(fieldId, data) {
  return request({
    url: '/api/metadata/field/' + fieldId + '/alias',
    method: 'post',
    data: data
  })
}

export function removeFieldAlias(fieldId, aliasId) {
  return request({
    url: '/api/metadata/field/' + fieldId + '/alias/' + aliasId,
    method: 'delete'
  })
}

export function getFieldAliasSuggestions(fieldId) {
  return request({
    url: '/api/metadata/field/' + fieldId + '/alias/suggestions',
    method: 'get'
  })
}

export function getAliasConflicts(alias, excludeFieldId) {
  return request({
    url: '/api/metadata/alias/conflicts',
    method: 'get',
    params: { alias, excludeFieldId }
  })
}

export function getFieldMetadata(id) {
  return request({
    url: '/api/metadata/field/' + id,
    method: 'get'
  })
}

export function addFieldMetadata(data) {
  return request({
    url: '/api/metadata/field',
    method: 'post',
    data: data
  })
}

export function updateFieldMetadata(data) {
  return request({
    url: '/api/metadata/field',
    method: 'put',
    data: data
  })
}

export function delFieldMetadata(ids) {
  return request({
    url: '/api/metadata/field/' + ids,
    method: 'delete'
  })
}

// ========== 原子指标管理 ==========
export function listAtomicMetric(query) {
  return request({
    url: '/api/metadata/metric/list',
    method: 'get',
    params: query
  })
}

export function getAtomicMetric(id) {
  return request({
    url: '/api/metadata/metric/' + id,
    method: 'get'
  })
}

export function addAtomicMetric(data) {
  return request({
    url: '/api/metadata/metric',
    method: 'post',
    data: data
  })
}

export function updateAtomicMetric(data) {
  return request({
    url: '/api/metadata/metric',
    method: 'put',
    data: data
  })
}

export function delAtomicMetric(ids) {
  return request({
    url: '/api/metadata/metric/' + ids,
    method: 'delete'
  })
}

// ========== 维度管理 ==========
export function listDimension(query) {
  return request({
    url: '/api/metadata/dimension/list',
    method: 'get',
    params: query
  })
}

export function getDimension(id) {
  return request({
    url: '/api/metadata/dimension/' + id,
    method: 'get'
  })
}

export function addDimension(data) {
  return request({
    url: '/api/metadata/dimension',
    method: 'post',
    data: data
  })
}

export function updateDimension(data) {
  return request({
    url: '/api/metadata/dimension',
    method: 'put',
    data: data
  })
}

export function delDimension(ids) {
  return request({
    url: '/api/metadata/dimension/' + ids,
    method: 'delete'
  })
}

// ========== 表关系（推荐 join）管理 ==========
export function listTableRelation(params) {
  return request({
    url: '/api/metadata/relation/list',
    method: 'get',
    params
  })
}

export function getTableRelation(id) {
  return request({
    url: '/api/metadata/relation/' + id,
    method: 'get'
  })
}

export function getRelationGraphData() {
  return request({
    url: '/api/metadata/relation/graph-data',
    method: 'get'
  })
}

export function addTableRelation(data) {
  return request({
    url: '/api/metadata/relation',
    method: 'post',
    data
  })
}

export function updateTableRelation(id, data) {
  return request({
    url: '/api/metadata/relation/' + id,
    method: 'put',
    data
  })
}

export function delTableRelation(ids) {
  return request({
    url: '/api/metadata/relation/' + ids,
    method: 'delete'
  })
}

// ========== 歧义优化 / 智能标注 ==========
export function listAmbiguity(params) {
  return request({
    url: '/api/metadata/ambiguity/list',
    method: 'get',
    params
  })
}

export function resolveAmbiguity(id) {
  return request({
    url: '/api/metadata/ambiguity/' + id + '/resolve',
    method: 'put'
  })
}

export function getAmbiguitySummary(params) {
  return request({
    url: '/api/metadata/ambiguity/summary',
    method: 'get',
    params
  })
}

// ========== Few-shot 示例管理（NL2SQL 参考示例） ==========
export function listFewshotExample(params) {
  return request({
    url: '/api/metadata/fewshot-examples/list',
    method: 'get',
    params
  })
}

export function getFewshotExample(id) {
  return request({
    url: '/api/metadata/fewshot-examples/' + id,
    method: 'get'
  })
}

export function addFewshotExample(data) {
  return request({
    url: '/api/metadata/fewshot-examples',
    method: 'post',
    data
  })
}

export function updateFewshotExample(data) {
  return request({
    url: '/api/metadata/fewshot-examples',
    method: 'put',
    data
  })
}

export function delFewshotExample(id) {
  return request({
    url: '/api/metadata/fewshot-examples/' + id,
    method: 'delete'
  })
}

export function updateFewshotEnabled(id, enabled) {
  return request({
    url: '/api/metadata/fewshot-examples/enabled',
    method: 'put',
    data: { id, enabled }
  })
}

export function batchUpdateFewshotEnabled(ids, enabled) {
  return request({
    url: '/api/metadata/fewshot-examples/batch-enabled',
    method: 'put',
    data: { ids, enabled }
  })
}

export function importFewshotFromFeedback(feedbackId) {
  return request({
    url: '/api/metadata/fewshot-examples/import-from-feedback',
    method: 'post',
    data: { feedbackId }
  })
}

// ========== 元数据自动补全（问数输入联想） ==========
export function metadataAutocomplete(keyword, limit = 10) {
  return request({
    url: '/api/metadata/autocomplete',
    method: 'get',
    params: { keyword: keyword || '', limit }
  })
}
