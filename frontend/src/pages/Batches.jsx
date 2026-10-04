import React, { useEffect, useState } from 'react'
import { api } from '../api.js'
import { Card, Modal, Field, Badge, fmt, fmtTime, useToast, useMeta, QC_LABEL } from '../ui.jsx'

export default function Batches() {
  const toast = useToast()
  const { meta } = useMeta()
  const [rows, setRows] = useState([])
  const [matFilter, setMatFilter] = useState('')
  const [showIn, setShowIn] = useState(false)
  const [form, setForm] = useState({
    materialId: '', spec: '', supplierId: '', qtyInbound: '', unitPrice: '', qcStatus: 'PENDING', remark: ''
  })

  const load = () => api.get('/api/batches' + (matFilter ? `?materialId=${matFilter}` : '')).then(setRows)
  useEffect(() => { load() }, [matFilter])

  const submit = async () => {
    if (!form.materialId || !form.qtyInbound) return toast('请选择材料并填写入库数量', 'err')
    try {
      await api.post('/api/batches/inbound', {
        ...form,
        materialId: Number(form.materialId),
        supplierId: form.supplierId ? Number(form.supplierId) : null,
        qtyInbound: Number(form.qtyInbound),
        unitPrice: form.unitPrice === '' ? 0 : Number(form.unitPrice)
      })
      toast('入库登记成功')
      setShowIn(false)
      setForm({ materialId: '', spec: '', supplierId: '', qtyInbound: '', unitPrice: '', qcStatus: 'PENDING', remark: '' })
      load()
    } catch (e) { toast(e.message, 'err') }
  }

  const setQc = async (id, qcStatus) => {
    try {
      await api.put(`/api/batches/${id}/qc`, { qcStatus })
      toast(`质检结果已更新为：${QC_LABEL[qcStatus]}`)
      load()
    } catch (e) { toast(e.message, 'err') }
  }

  return (
    <div>
      <Card title="入库批次与质检" hint="仓库登记批次、规格、供应商、入库数量、质检和单价；不合格批次不可发料"
            actions={<button className="btn primary" onClick={() => setShowIn(true)}>＋ 采购入库登记</button>} flush>
        <div className="card-bd">
          <div className="filters">
            <div className="f">
              <label>按材料筛选</label>
              <select value={matFilter} onChange={(e) => setMatFilter(e.target.value)}>
                <option value="">全部材料</option>
                {meta.materials.map((m) => <option key={m.id} value={m.id}>{m.name}</option>)}
              </select>
            </div>
          </div>
        </div>
        <div className="table-scroll">
          <table className="tbl">
            <thead><tr>
              <th>批次号</th><th>材料</th><th>规格</th><th>供应商</th>
              <th className="num">入库量</th><th className="num">已领</th><th className="num">退库</th>
              <th className="num">损耗</th><th className="num">盘点调整</th><th className="num">现场库存</th>
              <th className="num">单价</th><th className="num">入库金额</th><th>质检</th><th>入库时间</th><th>操作</th>
            </tr></thead>
            <tbody>
            {rows.map((b) => (
              <tr key={b.id}>
                <td className="code-link">{b.batchNo}</td>
                <td>{b.materialName}</td>
                <td>{b.spec}</td>
                <td>{b.supplierName || '—'}</td>
                <td className="num">{fmt(b.qtyInbound)}</td>
                <td className="num">{fmt(b.qtyIssued)}</td>
                <td className="num">{fmt(b.qtyReturned)}</td>
                <td className="num">{b.qtyWasted > 0 ? <span className="pos">{fmt(b.qtyWasted)}</span> : 0}</td>
                <td className="num">{b.qtyAdjust !== 0 ? fmt(b.qtyAdjust) : 0}</td>
                <td className="num"><b>{fmt(b.onHand)}</b></td>
                <td className="num">{fmt(b.unitPrice)}</td>
                <td className="num">{fmt(b.amount)}</td>
                <td><Badge status={b.qcStatus} label={QC_LABEL[b.qcStatus]} /></td>
                <td>{fmtTime(b.inboundTime)}</td>
                <td>
                  <div className="btn-row">
                    {b.qcStatus === 'PENDING' && <>
                      <button className="btn sm" onClick={() => setQc(b.id, 'PASSED')}>合格</button>
                      <button className="btn sm danger" onClick={() => setQc(b.id, 'FAILED')}>不合格</button>
                    </>}
                    {b.qcStatus !== 'PENDING' && <span className="muted" style={{ fontSize: 12 }}>已出质检</span>}
                  </div>
                </td>
              </tr>
            ))}
            </tbody>
          </table>
        </div>
      </Card>

      {showIn && (
        <Modal title="采购入库登记" onClose={() => setShowIn(false)}
               footer={<><button className="btn" onClick={() => setShowIn(false)}>取消</button>
                 <button className="btn primary" onClick={submit}>确认入库</button></>}>
          <div className="grid2">
            <Field label="材料 *">
              <select value={form.materialId} onChange={(e) => {
                const m = meta.materials.find((x) => x.id === Number(e.target.value))
                setForm({ ...form, materialId: e.target.value, spec: m?.spec || '' })
              }}>
                <option value="">请选择</option>
                {meta.materials.map((m) => <option key={m.id} value={m.id}>[{m.category}] {m.name}</option>)}
              </select>
            </Field>
            <Field label="规格"><input value={form.spec} onChange={(e) => setForm({ ...form, spec: e.target.value })} /></Field>
            <Field label="供应商">
              <select value={form.supplierId} onChange={(e) => setForm({ ...form, supplierId: e.target.value })}>
                <option value="">无（暂估）</option>
                {meta.suppliers.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </Field>
            <Field label="入库数量 *"><input type="number" value={form.qtyInbound}
              onChange={(e) => setForm({ ...form, qtyInbound: e.target.value })} /></Field>
            <Field label="单价（元）"><input type="number" value={form.unitPrice}
              onChange={(e) => setForm({ ...form, unitPrice: e.target.value })} /></Field>
            <Field label="质检状态">
              <select value={form.qcStatus} onChange={(e) => setForm({ ...form, qcStatus: e.target.value })}>
                <option value="PENDING">待检</option>
                <option value="PASSED">合格</option>
                <option value="CONDITIONAL">让步接收</option>
                <option value="FAILED">不合格</option>
              </select>
            </Field>
          </div>
          <Field label="备注（如补货延迟说明）"><input value={form.remark}
            onChange={(e) => setForm({ ...form, remark: e.target.value })} placeholder="例：供应商补货延迟 2 天到场" /></Field>
        </Modal>
      )}
    </div>
  )
}
