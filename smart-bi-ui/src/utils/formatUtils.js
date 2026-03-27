/**
 * 数值格式化工具
 * - formatNumber: 支持 decimal/percent/currency 等风格
 * - 大数简写: 1.2亿、1.2万
 * @author smart-bi
 */

/**
 * 格式化数字
 * @param {number|string} value - 待格式化值
 * @param {Object} options - 格式化选项
 * @param {string} [options.style='decimal'] - 'decimal'|'percent'|'currency'
 * @param {number} [options.minimumFractionDigits] - 最小小数位数
 * @param {number} [options.maximumFractionDigits] - 最大小数位数
 * @param {string} [options.currency='CNY'] - 货币代码（style=currency 时）
 * @param {string} [options.locale='zh-CN'] - 区域
 * @returns {string}
 */
export function formatNumber(value, options = {}) {
  const {
    style = 'decimal',
    minimumFractionDigits,
    maximumFractionDigits,
    currency = 'CNY',
    locale = 'zh-CN'
  } = options

  const num = parseFloat(value)
  if (Number.isNaN(num)) return ''

  const opts = { style }
  if (minimumFractionDigits != null) opts.minimumFractionDigits = minimumFractionDigits
  if (maximumFractionDigits != null) opts.maximumFractionDigits = maximumFractionDigits
  if (style === 'currency') opts.currency = currency

  try {
    return new Intl.NumberFormat(locale, opts).format(num)
  } catch (e) {
    return String(num)
  }
}

/**
 * 大数简写（万、亿）
 * @param {number|string} value - 待格式化值
 * @param {Object} options - 可选
 * @param {number} [options.fractionDigits=1] - 小数位数
 * @param {number} [options.wanThreshold=10000] - 达到此值显示“万”
 * @param {number} [options.yiThreshold=100000000] - 达到此值显示“亿”
 * @returns {string}
 */
export function formatLargeNumber(value, options = {}) {
  const {
    fractionDigits = 1,
    wanThreshold = 10000,
    yiThreshold = 100000000
  } = options

  const num = parseFloat(value)
  if (Number.isNaN(num)) return ''

  const abs = Math.abs(num)
  const sign = num < 0 ? '-' : ''

  if (abs >= yiThreshold) {
    const v = (abs / yiThreshold).toFixed(fractionDigits).replace(/\.?0+$/, '')
    return sign + v + '亿'
  }
  if (abs >= wanThreshold) {
    const v = (abs / wanThreshold).toFixed(fractionDigits).replace(/\.?0+$/, '')
    return sign + v + '万'
  }
  return String(num)
}

/**
 * 按 display_format 配置格式化（兼容后端字段）
 * @param {number|string} value
 * @param {string} [displayFormat] - 'decimal'|'percent'|'currency'|'large'
 * @param {Object} [opts] - 额外选项
 */
export function formatByDisplayFormat(value, displayFormat, opts = {}) {
  if (!displayFormat) return formatNumber(value, { style: 'decimal', ...opts })
  const low = String(displayFormat).toLowerCase()
  if (low === 'percent' || low === 'percentage') {
    return formatNumber(value, { style: 'percent', maximumFractionDigits: 2, ...opts })
  }
  if (low === 'currency' || low === 'money') {
    return formatNumber(value, { style: 'currency', minimumFractionDigits: 2, ...opts })
  }
  if (low === 'large' || low === 'compact') {
    return formatLargeNumber(value, opts)
  }
  return formatNumber(value, { style: 'decimal', ...opts })
}
