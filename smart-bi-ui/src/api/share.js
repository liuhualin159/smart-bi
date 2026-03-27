import request from '@/utils/request'

// 生成分享链接
export function generateShareLink(data) {
  return request({
    url: '/api/share/link',
    method: 'post',
    data: data
  })
}

// 访问分享链接（无需登录）
export function accessShareLink(shareKey, password) {
  return request({
    url: '/api/share/' + shareKey,
    method: 'get',
    params: { password }
  })
}

// 获取分享链接列表
export function getShareLinkList(query) {
  return request({
    url: '/api/share/list',
    method: 'get',
    params: query
  })
}

// 禁用分享链接
export function disableShareLink(id) {
  return request({
    url: '/api/share/disable/' + id,
    method: 'put'
  })
}

// 删除分享链接
export function delShareLink(ids) {
  return request({
    url: '/api/share/' + ids,
    method: 'delete'
  })
}
