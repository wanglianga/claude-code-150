import React, { useEffect, useState } from 'react'
import { api, getUser } from '../api.js'
import { Card, Stats, Modal, Badge, fmt, useToast, useMeta } from '../ui.jsx'

export default function Settlements() {
  const toast = useToast()
  const { meta } = useMeta()
  const user = getUser()
  const canGenerate = ['ROLE_PM', 'ROLE_BUDGET', 'ROLE_FINANCE'].includes(user.role)
  const canConfirm = ['ROLE_FINANCE', 'ROLE_PM'].includes(user.role)

  const [rows, setRows] = useState([])
  const [period, setPeriod] = useState('2026-10')
  const [genCrew, setGenCrew] = useState('')
  const [detail, setDetail] = useState(null)

  const load = () => api.get('/api/settlements').then(setRows).catch(() => setRows([]))
  useEffect(() => { load() }, [])

  const generate = async () => {
    try {
      await api.post('/api/settlements/generate', {
        period, crewId: genCrew ? Number(genCrew) : null
      })
      toast(genCrew ? '班组核算单已生成' : '已为全部班组生成核算单')
      load()
    } catch (e) { toast(e.message, 'err') }
  }
  const confirm = async (s) => {
    try {
      await api.put(`/api/settlements/${s.id}/confirm`, {})
      toast('核算单已确认'); setDetail(null); load()
    } catch (e) { toast(e.message, 'err') }
  }

  const shown = rows.filter((s) => !period || s.period === period)
  const sum = (k) => shown.reduce((a, s) => a + (s[k] || 0), 0)

  return (
    <div>
      <Stats items={[
        { label: '实际消耗合计', value: fmt(sum('consumedAmount')), tone: 'brand' },
        { label: '班组责任损耗', value: fmt(sum('crewWasteAmount')), tone: 'red' },
        { label: '材料偏差合计', value: fmt(sum('varianceAmount')),
          tone: sum('varianceAmount') > 0 ? 'red' : 'green' },
        { label: '班组扣款合计', value: fmt(sum('deductionAmount')), tone: 'red' },
        { label: '变更索赔合计', value: fmt(sum('claimAmount')), tone: 'blue' }
      ]} />

      <Card title="班组月度材料核算"
            hint="按预算、实际领用、退库、损耗、责任归属生成班组材料成本；扣款=班组责任损耗+正偏差"
            actions={
              <div className="filters">
                <div className="f">
                  <label>核算月份</label>
                  <input type="month" value={period} onChange={(e) => setPeriod(e.target.value)} />
                </div>
                {canGenerate && <>
                  <div className="f">
                    <label>生成范围</label>
                    <select value={genCrew} onChange={(e) => setGenCrew(e.target.value)}>
                      <option value="">全部班组</option>
                      {meta.crews.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                    </select>
                  </div>
                  <button className="btn primary" onClick={generate}>生成/重算核算单</button>
                </>}
              </div>
            } flush>
        <div className="table-scroll">
          <table className="tbl">
            <thead><tr>
              <th>核算单号</th><th>月份</th><th>班组</th>
              <th className="num">预算金额</th><th className="num">领用金额</th><th className="num">退库金额</th>
              <th className="num">实际消耗</th><th className="num">班组责任损耗</th>
              <th className="num">供应商质量</th><th className="num">设计/公司承担</th>
              <th className="num">材料偏差</th><th className="num">班组扣款</th>
              <th className="num">变更索赔</th><th>状态</th><th>操作</th>
            </tr></thead>
            <tbody>
            {shown.map((s) => {
              const c = meta.crews.find((x) => x.id === s.crewId)
              return (
                <tr key={s.id}>
                  <td className="code-link" onClick={() => setDetail(s)}>{s.code}</td>
                  <td>{s.period}</td>
                  <td><b>{c?.name}</b></td>
                  <td className="num">{fmt(s.budgetAmount)}</td>
                  <td className="num">{fmt(s.issuedAmount)}</td>
                  <td className="num neg">{fmt(s.returnedAmount)}</td>
                  <td className="num"><b>{fmt(s.consumedAmount)}</b></td>
                  <td className="num pos">{fmt(s.crewWasteAmount)}</td>
                  <td className="num" style={{ color: '#92400e' }}>{fmt(s.supplierWasteAmount)}</td>
                  <td className="num muted">{fmt(s.otherWasteAmount)}</td>
                  <td className={`num ${s.varianceAmount > 0 ? 'pos' : 'neg'}`}>{fmt(s.varianceAmount)}</td>
                  <td className="num pos">{fmt(s.deductionAmount)}</td>
                  <td className="num" style={{ color: 'var(--blue)' }}>{fmt(s.claimAmount)}</td>
                  <td><Badge status={s.status} /></td>
                  <td>
                    <button className="btn sm ghost" onClick={() => setDetail(s)}>明细</button>
                    {s.status === 'DRAFT' && canConfirm &&
                      <button className="btn sm primary" onClick={() => confirm(s)}>财务确认</button>}
                  </td>
                </tr>
              )
            })}
            {shown.length === 0 && <tr><td colSpan={15}><div className="empty">
              {canGenerate ? '该月份尚未生成核算单，点击右上角"生成/重算核算单"' : '暂无核算单'}</div></td></tr>}
            </tbody>
          </table>
        </div>
      </Card>

      {detail && (
        <Modal title={`班组材料成本核算单 ${detail.code}`} wide onClose={() => setDetail(null)}
               footer={<><button className="btn" onClick={() => setDetail(null)}>关闭</button>
                 {detail.status === 'DRAFT' && canConfirm &&
                   <button className="btn primary" onClick={() => confirm(detail)}>财务确认</button>}</>}>
          <div className="kpi-note" style={{ lineHeight: 2.1 }}>
            <div>班组：<b>{meta.crews.find((c) => c.id === detail.crewId)?.name}</b>　核算月份：<b>{detail.period}</b>　状态：<Badge status={detail.status} /></div>
            <div>① 预算金额：<b>{fmt(detail.budgetAmount)}</b> 元</div>
            <div>② 实际领用：<b>{fmt(detail.issuedAmount)}</b> 元　③ 完好退库冲减：<b className="neg">{fmt(detail.returnedAmount)}</b> 元</div>
            <div>④ 实际消耗（②−③）：<b style={{ color: 'var(--brand-dark)' }}>{fmt(detail.consumedAmount)}</b> 元</div>
            <div>⑤ 班组责任损耗（含损坏退库）：<b className="pos">{fmt(detail.crewWasteAmount)}</b> 元；
              供应商质量损耗 <b>{fmt(detail.supplierWasteAmount)}</b> 元（向供应商索赔，不扣班组）；
              设计变更/公司承担 <b>{fmt(detail.otherWasteAmount)}</b> 元</div>
            <div>⑥ 材料偏差（④−①）：<b className={detail.varianceAmount > 0 ? 'pos' : 'neg'}>{fmt(detail.varianceAmount)}</b> 元</div>
            <div>⑦ 班组材料扣款 = 班组责任损耗 + 正偏差 = <b className="pos">{fmt(detail.deductionAmount)}</b> 元</div>
            <div>⑧ 变更签证索赔：<b style={{ color: 'var(--blue)' }}>{fmt(detail.claimAmount)}</b> 元</div>
            <div className="muted">制单：{detail.generatedBy || '—'}　确认：{detail.confirmedBy || '待财务确认'}</div>
          </div>
        </Modal>
      )}
    </div>
  )
}
