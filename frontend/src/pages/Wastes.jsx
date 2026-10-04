import React, { useEffect, useState } from 'react'
import { api } from '../api.js'
import { Card, Modal, Field, Badge, fmt, fmtTime, useToast, useMeta, WASTE_TYPE, RESPONSIBILITY } from '../ui.jsx'

export default function Wastes() {
  const toast = useToast()
  const { meta } = useMeta()
  const [rows, setRows] = useState([])
  const [show, setShow] = useState(false)
  const [batches, setBatches] = useState([])
  const [respFilter, setRespFilter] = useState('')
  const [f, setF] = useState({
    materialId: '', batchId: '', crewId: '', building: '1#楼', area: '', process: '',
    qty: '', wasteType: 'BREAKAGE', responsibility: 'CREW', description: ''
  })

  const load = () => api.get('/api/wastes').then(setRows)
  useEffect(() => { load() }, [])
  useEffect(() => { if (show) api.get('/api/batches').then(setBatches) }, [show])

  const submit = async () => {
    if (!f.materialId || !f.qty || !f.wasteType) return toast('请选择材料、填写损耗数量与类型', 'err')
    try {
      await api.post('/api/wastes', {
        ...f,
        materialId: Number(f.materialId),
        batchId: f.batchId ? Number(f.batchId) : null,
        crewId: f.crewId ? Number(f.crewId) : null,
        qty: Number(f.qty)
      })
      toast('损耗已登记并核销批次库存')
      setShow(false); load()
    } catch (e) { toast(e.message, 'err') }
  }

  const shown = rows.filter((w) => !respFilter || w.responsibility === respFilter)
  const total = rows.reduce((s, w) => s + w.amount, 0)
  const respTone = { CREW: 'red', SUPPLIER: 'amber', DESIGN: 'blue', COMPANY: 'gray', NATURE: 'gray' }

  return (
    <div>
      <Card title="损耗登记与核销"
            hint={`瓷砖破损、砂浆过期、保管不当、施工错误、供应商质量问题等；损耗合计 ${fmt(total)} 元`}
            actions={<div className="filters">
              <div className="f">
                <label>责任归属</label>
                <select value={respFilter} onChange={(e) => setRespFilter(e.target.value)}>
                  <option value="">全部</option>
                  {Object.entries(RESPONSIBILITY).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                </select>
              </div>
              <button className="btn primary" onClick={() => setShow(true)}>＋ 登记损耗</button>
            </div>} flush>
        <div className="table-scroll">
          <table className="tbl">
            <thead><tr>
              <th>单号</th><th>材料</th><th>楼栋/工序</th><th>责任班组</th>
              <th className="num">数量</th><th className="num">单价</th><th className="num">金额</th>
              <th>损耗类型</th><th>责任归属</th><th>说明</th><th>登记人/时间</th>
            </tr></thead>
            <tbody>
            {shown.map((w) => (
              <tr key={w.id}>
                <td className="code-link">{w.code}</td>
                <td><b>{w.materialName}</b></td>
                <td>{w.building}<div className="muted" style={{ fontSize: 12 }}>{w.process}</div></td>
                <td>{w.crewName || '—'}</td>
                <td className="num">{fmt(w.qty)} {w.unit}</td>
                <td className="num">{fmt(w.unitPrice)}</td>
                <td className="num pos">{fmt(w.amount)}</td>
                <td><Badge status="DRAFT" label={WASTE_TYPE[w.wasteType] || w.wasteType} tone="amber" /></td>
                <td><Badge status="DRAFT" label={RESPONSIBILITY[w.responsibility]} tone={respTone[w.responsibility]} /></td>
                <td className="desc-cell">{w.description}</td>
                <td style={{ fontSize: 12 }}>{w.recorder}<div className="muted">{fmtTime(w.createdTime)}</div></td>
              </tr>
            ))}
            </tbody>
          </table>
        </div>
      </Card>

      {show && (
        <Modal title="登记现场损耗" wide onClose={() => setShow(false)}
               footer={<><button className="btn" onClick={() => setShow(false)}>取消</button>
                 <button className="btn primary" onClick={submit}>确认核销</button></>}>
          <div className="grid3">
            <Field label="材料 *">
              <select value={f.materialId} onChange={(e) => setF({ ...f, materialId: e.target.value, batchId: '' })}>
                <option value="">请选择</option>
                {meta.materials.map((m) => <option key={m.id} value={m.id}>{m.name}（{m.unit}）</option>)}
              </select>
            </Field>
            <Field label="核销批次（不选则只记账不扣库存）">
              <select value={f.batchId} onChange={(e) => setF({ ...f, batchId: e.target.value })}>
                <option value="">不指定批次</option>
                {batches.filter((b) => !f.materialId || b.materialId === Number(f.materialId))
                  .map((b) => <option key={b.id} value={b.id}>{b.batchNo}｜库存{fmt(b.onHand)}｜{fmt(b.unitPrice)}元</option>)}
              </select>
            </Field>
            <Field label="责任班组">
              <select value={f.crewId} onChange={(e) => setF({ ...f, crewId: e.target.value })}>
                <option value="">无（项目/供应商责任）</option>
                {meta.crews.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </Field>
            <Field label="楼栋 *"><input value={f.building} onChange={(e) => setF({ ...f, building: e.target.value })} /></Field>
            <Field label="工序"><input value={f.process} onChange={(e) => setF({ ...f, process: e.target.value })} /></Field>
            <Field label="损耗数量 *"><input type="number" value={f.qty} onChange={(e) => setF({ ...f, qty: e.target.value })} /></Field>
            <Field label="损耗类型 *">
              <select value={f.wasteType} onChange={(e) => setF({ ...f, wasteType: e.target.value })}>
                {Object.entries(WASTE_TYPE).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
            </Field>
            <Field label="责任归属 *">
              <select value={f.responsibility} onChange={(e) => setF({ ...f, responsibility: e.target.value })}>
                {Object.entries(RESPONSIBILITY).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
            </Field>
            <Field label="施工区域"><input value={f.area} onChange={(e) => setF({ ...f, area: e.target.value })} /></Field>
          </div>
          <Field label="情况说明（设计变更/施工错误/保管不当/质量问题的经过）">
            <textarea rows={3} value={f.description} onChange={(e) => setF({ ...f, description: e.target.value })} />
          </Field>
          <div className="kpi-note">
            口径：<b>班组责任</b>进入月底扣款；<b>供应商责任</b>进入供应商质量索赔；
            <b>设计变更</b>与变更签证关联，进入变更索赔；<b>项目承担/不可抗力</b>由项目成本消化。
          </div>
        </Modal>
      )}
    </div>
  )
}
