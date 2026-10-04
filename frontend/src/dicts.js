// 业务字典
export const ROLES = {
  ADMIN: '系统管理员',
  PROJECT_MANAGER: '项目经理',
  WAREHOUSE: '仓管员',
  CREW_LEADER: '班组长',
  FINANCE: '财务',
  BUDGET: '预算员'
}

export const MATERIAL_CATEGORIES = ['钢筋辅材', '防水卷材', '瓷砖', '线管', '砂浆', '五金', '安全防护用品']

export const REQ_STATUS = {
  DRAFT: '草稿',
  APPLIED: '已申请',
  ISSUED: '已发料',
  CONFIRMED: '已确认',
  REJECTED: '已驳回'
}

export const REQ_STATUS_COLOR = {
  DRAFT: 'default',
  APPLIED: 'processing',
  ISSUED: 'warning',
  CONFIRMED: 'success',
  REJECTED: 'error'
}

export const PRIORITY = { NORMAL: '正常', URGENT: '夜间紧急' }

export const RETURN_REASON = {
  WRONG_SPEC: '规格领错',
  CHANGE: '施工变更',
  EXTRA_APPEND: '临时追加未用',
  SURPLUS: '现场余料'
}

export const DISPOSITION = { REUSE: '可复用回库', SCRAP: '报废' }

export const LOSS_REASON = {
  DESIGN_CHANGE: '设计变更',
  CONSTRUCTION_ERROR: '施工错误',
  POOR_STORAGE: '保管不当',
  SUPPLIER_QUALITY: '供应商质量',
  NORMAL: '合理损耗'
}

export const RESPONSIBILITY = {
  CREW: '班组承担（扣款）',
  SUPPLIER: '供应商承担（索赔）',
  PROJECT: '项目承担'
}

export const RESPONSIBILITY_SHORT = { CREW: '班组', SUPPLIER: '供应商', PROJECT: '项目' }

export const QC_STATUS = { PENDING: '待检', PASSED: '合格', FAILED: '不合格' }

export const QC_COLOR = { PENDING: 'gold', PASSED: 'green', FAILED: 'red' }

export const CHANGE_STATUS = { DRAFT: '待审批', APPROVED: '已批准', REJECTED: '已驳回' }

export const CHANGE_TYPE = { DESIGN: '设计变更', CONSTRUCTION: '施工变更' }

export function money(v) {
  const n = Number(v || 0)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function num(v, digits = 2) {
  return Number(v || 0).toLocaleString('zh-CN', { maximumFractionDigits: digits })
}
