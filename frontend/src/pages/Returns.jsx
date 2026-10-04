import React, { useEffect, useState } from 'react'
import { api, getUser } from '../api.js'
import { Card, Modal, Field, Badge, fmt, fmtTime, useToast, useMeta, RETURN_REASON, CONDITION } from '../ui.jsx'

export default function Returns() {
  const toast = useToast()
  const { meta } = useMeta()
  const user = getUser()
  const canCreate = ['ROLE_FOREMAN', 'ROLE_KEEPER', 'ROLE_PM'].includes(user.role)
  const canConfirm = ['ROLE_KEEPER', 'ROLE_PM'].includes(user.role)

  const [rows, setRows] = useState([])
  const [status, setStatus] = useState('')
  const [show, setShow] = useState(false)
  const [detail, setDetail] = useState(null)
  const blank = () => ({ materialId: '', batchId: '', qty: '', conditionType: 'GOOD', price: '' })
  const [f, setF] = useState({
    crewId: user.crewId || '', requisitionId: '', building: '1#楼', area: '', process: '',
    reasonType: 'SURPLUS', remark: '', items: [blank()]
  })
  const [reqOptions, setReqOptions] = useState([])

  const load = () => {
    const q = new URLSearchParams()
    if (status) q.set('status', status)
    if (user.role === 'ROLE_FOREMAN') q.set('crewId', user.crewId)
    api.get('/api/returns?' + q.toString()).then(setRows)
  }
  useEffect(() => { load() }, [status])
  useEffect(() => {
    if (show) api.get('/api/requisitions?status=ISSUED').then(setReqOptions).catch(() => {})
  }, [show])

  const batchesOf = (materialId) => {
    // 通过批次列表拉取（未专门做按材料过滤接口，前端筛选）
    return window.__batchesCache?.filter((b) => b.materialId === Number(materialId)) || []
  }
  const [batchCache, setBatchCache] = useState([])
  useEffect(() => { if (show) api.get('/api/batches').then((d) => { setBatchCache(d); window.__batchesCache = d }) }, [show])

  const submit = async () => {
    if (!f.crewId || f.items.some((i) => !i.materialId || !i.qty)) return toast('请完善班组与退库明细', 'err')
    try {
      await api.post('/api/returns', {
        ...f,
        crewId: Number(f.crewId),
        requisitionId: f.requisitionId ? Number(f.requisitionId) : null,
        items: f.items.map((i) => ({
          materialId: Number(i.materialId),
          batchId: i.batchId ? Number(i.batchId) : null,
          qty: Number(i.qty),
          conditionType: i.conditionType,
          price: i.price === '' ? undefined : Number(i.price)
        }))
      })
      toast('退库单已提交，等待仓管确认')
      setShow(false); setF({ ...f, items: [blank()] }); load()
    } catch (e) { toast(e.message, 'err') }
  }

  const confirm = async (r) => {
    try {
      await api.put(`/api/returns/${r.id}/confirm`, {})
      toast('退库已确认，完好材料回补批次库存')
      setDetail(null); load()
    } catch (e) { toast(e.message, 'err') }
  }
  const reject = async (id) => {
    if (!window.confirm('确认驳回该退库单？')) return
    await api.put(`/api/returns/${id}/reject`, {}); toast('已驳回'); load()
  }

  return (
    <Card title="退库单" hint="临时追加多余 / 规格领错 / 施工变更退料；损坏退回不回补库存，计入班组责任损耗"
          actions={<div className="filters">
            <div className="f">
              <label>状态</label>
              <select value={status} onChange={(e) => setStatus(e.target.value)}>
                <option value="">全部</option>
                <option value="SUBMITTED">待确认</option>
                <option value="CONFIRMED">已确认</option>
                <option value="REJECTED">已驳回</option>
              </select>
            </div>
            {canCreate && <button className="btn primary" onClick={() => setShow(true)}>＋ 办理退库</button>}
          </div>} flush>
      <div className="table-scroll">
        <table className="tbl">
          <thead><tr>
            <th>退库单号</th><th>班组</th><th>楼栋/工序</th><th>退库原因</th>
            <th>材料明细</th><th className="num">金额</th><th>状态</th>
            <th>申请人/确认人</th><th>操作</th>
          </tr></thead>
          <tbody>
          {rows.map((r) => (
            <tr key={r.id}>
              <td className="code-link" onClick={() => setDetail(r)}>{r.code}</td>
              <td>{r.crewName}</td>
              <td>{r.building}<div className="muted" style={{ fontSize: 12 }}>{r.process}</div></td>
              <td><Badge status="DRAFT" label={RETURN_REASON[r.reasonType]} tone="blue" /></td>
              <td style={{ fontSize: 12.5 }}>
                {r.items.map((i) => (
                  <div key={i.materialId}>{i.materialName} ×{fmt(i.qty)}
                    {i.conditionType === 'DAMAGED' && <span className="tag-mini" style={{ marginLeft: 6 }}>损坏</span>}
                  </div>
                ))}
              </td>
              <td className="num">{fmt(r.totalAmount)}</td>
              <td><Badge status={r.status} /></td>
              <td style={{ fontSize: 12 }}>{r.applicant}<div className="muted">{r.confirmedBy || '—'}</div></td>
              <td>
                {r.status === 'SUBMITTED' && canConfirm && (
                  <div className="btn-row">
                    <button className="btn sm primary" onClick={() => confirm(r)}>确认入库</button>
                    <button className="btn sm danger" onClick={() => reject(r.id)}>驳回</button>
                  </div>
                )}
                {r.status !== 'SUBMITTED' && <button className="btn sm ghost" onClick={() => setDetail(r)}>查看</button>}
              </td>
            </tr>
          ))}
          </tbody>
        </table>
      </div>

      {show && (
        <Modal title="办理退库" wide onClose={() => setShow(false)}
               footer={<><button className="btn" onClick={() => setShow(false)}>取消</button>
                 <button className="btn primary" onClick={submit}>提交退库单</button></>}>
          <div className="grid3">
            <Field label="班组 *">
              <select value={f.crewId} disabled={user.role === 'ROLE_FOREMAN'} onChange={(e) => setF({ ...f, crewId: e.target.value })}>
                <option value="">请选择</option>
                {meta.crews.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </Field>
            <Field label="楼栋 *"><input value={f.building} onChange={(e) => setF({ ...f, building: e.target.value })} /></Field>
            <Field label="工序 *"><input value={f.process} onChange={(e) => setF({ ...f, process: e.target.value })} /></Field>
            <Field label="施工区域"><input value={f.area} onChange={(e) => setF({ ...f, area: e.target.value })} /></Field>
            <Field label="关联领料单">
              <select value={f.requisitionId} onChange={(e) => setF({ ...f, requisitionId: e.target.value })}>
                <option value="">无（无单退库）</option>
                {reqOptions.filter((r) => !f.crewId || r.crewId === Number(f.crewId))
                  .map((r) => <option key={r.id} value={r.id}>{r.code} · {r.process}</option>)}
              </select>
            </Field>
            <Field label="退库原因 *">
              <select value={f.reasonType} onChange={(e) => setF({ ...f, reasonType: e.target.value })}>
                {Object.entries(RETURN_REASON).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
            </Field>
          </div>
          <Field label="备注"><input value={f.remark} onChange={(e) => setF({ ...f, remark: e.target.value })}
            placeholder="规格领错/施工变更/临时追加的具体说明" /></Field>

          <div style={{ margin: '8px 0 6px', fontWeight: 600 }}>退库明细（损坏退回请选择"损坏(班组责任)"，不回补库存）</div>
          <table className="tbl">
            <thead><tr><th>材料</th><th style={{ width: 170 }}>退回批次</th>
              <th style={{ width: 110 }}>数量</th><th style={{ width: 170 }}>完好程度</th>
              <th style={{ width: 110 }}>单价</th><th style={{ width: 60 }}></th></tr></thead>
            <tbody>
            {f.items.map((it, idx) => {
              const set = (patch) => {
                const items = f.items.slice()
                items[idx] = { ...items[idx], ...patch }
                setF({ ...f, items })
              }
              return (
                <tr key={idx}>
                  <td>
                    <select value={it.materialId} onChange={(e) => set({ materialId: e.target.value, batchId: '' })}>
                      <option value="">请选择</option>
                      {meta.materials.map((m) => <option key={m.id} value={m.id}>{m.name}</option>)}
                    </select>
                  </td>
                  <td>
                    <select value={it.batchId} onChange={(e) => set({ batchId: e.target.value })}>
                      <option value="">自动匹配批次</option>
                      {batchesOf(it.materialId).map((b) =>
                        <option key={b.id} value={b.id}>{b.batchNo}｜库存{fmt(b.onHand)}</option>)}
                    </select>
                  </td>
                  <td><input type="number" value={it.qty} onChange={(e) => set({ qty: e.target.value })} /></td>
                  <td>
                    <select value={it.conditionType} onChange={(e) => set({ conditionType: e.target.value })}>
                      {Object.entries(CONDITION).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                    </select>
                  </td>
                  <td><input type="number" placeholder="取批次价" value={it.price} onChange={(e) => set({ price: e.target.value })} /></td>
                  <td>{f.items.length > 1 && <button className="btn sm danger"
                    onClick={() => setF({ ...f, items: f.items.filter((_, i) => i !== idx) })}>删</button>}</td>
                </tr>
              )
            })}
            </tbody>
          </table>
          <button className="btn" style={{ marginTop: 10 }} onClick={() => setF({ ...f, items: [...f.items, blank()] })}>＋ 增加材料</button>
        </Modal>
      )}

      {detail && (
        <Modal title={`退库单 ${detail.code}`} wide onClose={() => setDetail(null)}
               footer={<><button className="btn" onClick={() => setDetail(null)}>关闭</button>
                 {detail.status === 'SUBMITTED' && canConfirm &&
                   <button className="btn primary" onClick={() => confirm(detail)}>确认入库</button>}</>}>
          <div className="kpi-note" style={{ marginBottom: 10 }}>
            {detail.crewName} · {detail.building} {detail.area} · {detail.process} ·
            原因：<b>{RETURN_REASON[detail.reasonType]}</b> · 状态：<Badge status={detail.status} />
            <div style={{ marginTop: 6 }}>{detail.remark}</div>
          </div>
          <table className="tbl">
            <thead><tr><th>材料</th><th>批次/数量</th><th>完好程度</th><th className="num">单价</th><th className="num">金额</th></tr></thead>
            <tbody>
            {detail.items.map((i) => (
              <tr key={i.materialId}>
                <td>{i.materialName}</td>
                <td>{i.qty}（批次 #{i.batchId || '自动'}）</td>
                <td>{i.conditionType === 'GOOD'
                  ? <Badge status="PASSED" label="完好，回补库存" />
                  : <Badge status="FAILED" label="损坏，班组责任" />}</td>
                <td className="num">{fmt(i.price)}</td>
                <td className="num">{fmt(i.amount)}</td>
              </tr>
            ))}
            </tbody>
          </table>
        </Modal>
      )}
    </Card>
  )
}
