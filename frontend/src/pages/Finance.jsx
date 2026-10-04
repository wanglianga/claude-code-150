import React, { useEffect, useState } from 'react'
import { api } from '../api.js'
import { Card, Stats, Badge, fmt, fmtTime } from '../ui.jsx'

const CHANGE_LABEL = { DESIGN: '设计变更', CONSTRUCTION: '施工变更', OWNER: '业主要求' }

export default function Finance() {
  const [data, setData] = useState(null)
  useEffect(() => { api.get('/api/analytics/finance').then(setData).catch(() => {}) }, [])

  return (
    <div>
      <Stats items={[
        { label: '供应商入库总额', value: fmt(data?.supplierTotalInbound), tone: 'blue' },
        { label: '供应商应付（扣不合格）', value: fmt(data?.supplierTotalPayable), tone: 'brand' },
        { label: '供应商质量索赔', value: fmt(data?.supplierQualityClaim), tone: 'red' },
        { label: '班组材料扣款合计', value: fmt(data?.crewTotalDeduction), tone: 'red' },
        { label: '变更签证索赔合计', value: fmt(data?.visaTotalClaim), tone: 'blue' }
      ]} />

      <Card title="供应商结算台账" hint="按入库批次金额汇总；质检不合格批次金额从应付款中扣减" flush>
        <div className="table-scroll">
          <table className="tbl">
            <thead><tr>
              <th>供应商</th><th className="num">入库金额</th><th className="num">质检不合格金额</th>
              <th className="num">建议应付金额</th><th>结算提示</th>
            </tr></thead>
            <tbody>
            {(data?.suppliers || []).map((s) => (
              <tr key={s.supplierId}>
                <td><b>{s.supplierName}</b></td>
                <td className="num">{fmt(s.inboundAmount)}</td>
                <td className="num pos">{fmt(s.qcRejectedAmount)}</td>
                <td className="num"><b>{fmt(s.payableAmount)}</b></td>
                <td>{s.qcRejectedAmount > 0
                  ? <Badge status="FAILED" label="有不合格批次，结算扣款" />
                  : <Badge status="PASSED" label="质检无异常" />}</td>
              </tr>
            ))}
            </tbody>
          </table>
        </div>
      </Card>

      <div className="grid2">
        <Card title="班组扣款台账（取各班组最新月份核算单）" flush>
          <div className="table-scroll">
            <table className="tbl">
              <thead><tr>
                <th>班组</th><th>月份</th><th className="num">实际消耗</th>
                <th className="num">责任损耗</th><th className="num">材料偏差</th>
                <th className="num">应扣金额</th><th>状态</th>
              </tr></thead>
              <tbody>
              {(data?.crews || []).map((c) => (
                <tr key={c.crewId}>
                  <td><b>{c.crewName}</b></td>
                  <td>{c.period}</td>
                  <td className="num">{fmt(c.consumedAmount)}</td>
                  <td className="num pos">{fmt(c.crewWasteAmount)}</td>
                  <td className={`num ${c.varianceAmount > 0 ? 'pos' : 'neg'}`}>{fmt(c.varianceAmount)}</td>
                  <td className="num pos"><b>{fmt(c.deductionAmount)}</b></td>
                  <td><Badge status={c.status} /></td>
                </tr>
              ))}
              {(!data?.crews || data.crews.length === 0) && (
                <tr><td colSpan={7}><div className="empty">尚未生成班组核算单</div></td></tr>
              )}
              </tbody>
            </table>
          </div>
        </Card>

        <Card title="变更索赔台账" flush>
          <div className="table-scroll">
            <table className="tbl">
              <thead><tr>
                <th>签证号</th><th>楼栋/工序</th><th>类型</th><th className="num">索赔金额</th>
                <th>状态</th><th>时间</th>
              </tr></thead>
              <tbody>
              {(data?.visas || []).map((v) => (
                <tr key={v.code}>
                  <td className="code-link">{v.code}</td>
                  <td>{v.building}<div className="muted" style={{ fontSize: 12 }}>{v.process}</div></td>
                  <td>{CHANGE_LABEL[v.changeType]}</td>
                  <td className="num"><b>{fmt(v.claimAmount)}</b></td>
                  <td><Badge status={v.status} /></td>
                  <td>{fmtTime(v.createdTime)}</td>
                </tr>
              ))}
              </tbody>
            </table>
          </div>
        </Card>
      </div>
    </div>
  )
}
