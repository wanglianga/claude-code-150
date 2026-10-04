import { useEffect, useMemo, useState } from 'react'
import {
  Card, Table, Button, Tag, Space, Modal, Form, Select, InputNumber, Input, App,
  Drawer, Alert, Checkbox, Typography, Badge, Popconfirm
} from 'antd'
import api from '../api.js'
import { useMasters } from '../useMasters.js'
import { REQ_STATUS, REQ_STATUS_COLOR, PRIORITY, money, num } from '../dicts.js'

export default function RequisitionPage({ user }) {
  const { message } = App.useApp()
  const m = useMasters()
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [statusFilter, setStatusFilter] = useState()
  const [applyOpen, setApplyOpen] = useState(false)
  const [detail, setDetail] = useState(null)
  const [issueData, setIssueData] = useState(null) // precheck payload
  const [issueForm, setIssueForm] = useState({ lines: {}, allowOverBudget: false, issueRemark: '' })
  const [rejectTarget, setRejectTarget] = useState(null)
  const [rejectReason, setRejectReason] = useState('')
  const [form] = Form.useForm()

  const isCrew = user.role === 'CREW_LEADER'
  const canIssue = user.role === 'WAREHOUSE' || user.role === 'ADMIN'

  const load = async () => {
    setLoading(true)
    try {
      let data = await api.get('/requisitions', { params: statusFilter ? { status: statusFilter } : {} })
      if (isCrew) data = data.filter((r) => r.crewId === user.crewId)
      setRows(data)
    } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [statusFilter])

  const matName = (id) => m.materials.find((x) => x.id === id)?.name || ''
  const matUnit = (id) => m.materials.find((x) => x.id === id)?.unit || ''

  const submitApply = async () => {
    const v = await form.validateFields()
    await api.post('/requisitions', {
      crewId: isCrew ? user.crewId : v.crewId,
      buildingId: v.buildingId,
      processId: v.processId,
      workArea: v.workArea,
      drawingNo: v.drawingNo,
      priority: v.priority || 'NORMAL',
      applyRemark: v.applyRemark,
      items: v.items.map((x) => ({ materialId: x.materialId, applyQty: x.applyQty, remark: x.remark }))
    })
    message.success('领料申请已提交，等待仓管核对发料')
    setApplyOpen(false)
    form.resetFields()
    load()
  }

  const openIssue = async (r) => {
    const pre = await api.get(`/requisitions/${r.id}/precheck`)
    const lines = {}
    pre.items.forEach((i) => {
      lines[i.itemId] = {
        batchId: i.availableBatches[0]?.id,
        issuedQty: Number(i.applyQty)
      }
    })
    setIssueData(pre)
    setIssueForm({ lines, allowOverBudget: false, issueRemark: pre.requisition.priority === 'URGENT' ? '夜间紧急抢工发料' : '' })
  }

  const submitIssue = async () => {
    const req = issueData.requisition
    const payload = {
      lines: issueData.items.map((i) => ({
        itemId: i.itemId,
        batchId: issueForm.lines[i.itemId]?.batchId,
        issuedQty: issueForm.lines[i.itemId]?.issuedQty
      })),
      issueRemark: issueForm.issueRemark,
      allowOverBudget: issueForm.allowOverBudget
    }
    await api.post(`/requisitions/${req.id}/issue`, payload)
    message.success('发料成功，库存已扣减，等待班组长确认')
    setIssueData(null)
    load()
  }

  const confirm = async (r) => {
    await api.post(`/requisitions/${r.id}/confirm`)
    message.success('班组长已确认，材料进入现场消耗')
    setDetail(null)
    load()
  }

  const reject = async () => {
    if (!rejectReason.trim()) {
      message.warning('请填写驳回原因')
      return
    }
    await api.post(`/requisitions/${rejectTarget.id}/reject`, { reason: rejectReason })
    message.success('已驳回')
    setRejectTarget(null)
    setRejectReason('')
    load()
  }

  const columns = [
    { title: '领料单号', dataIndex: 'reqNo', width: 175 },
    {
      title: '优先级', dataIndex: 'priority', width: 90,
      render: (v) => v === 'URGENT'
        ? <Tag color="red">夜间紧急</Tag>
        : <Tag>正常</Tag>
    },
    { title: '状态', dataIndex: 'status', width: 90,
      render: (v) => <Tag color={REQ_STATUS_COLOR[v]}>{REQ_STATUS[v]}</Tag> },
    { title: '楼栋', dataIndex: 'buildingName', width: 100 },
    { title: '工序', dataIndex: 'processName', width: 100 },
    { title: '班组', dataIndex: 'crewName', width: 110 },
    { title: '施工区域/图纸', render: (_, r) => (
      <div><div>{r.workArea}</div><div className="muted" style={{ fontSize: 12 }}>图纸 {r.drawingNo}</div></div>
    ) },
    { title: '材料种类', width: 90, align: 'right', render: (_, r) => r.items?.length ?? 0 },
    { title: '申请时间', dataIndex: 'appliedAt', width: 165 },
    {
      title: '操作', width: 200, fixed: 'right',
      render: (_, r) => (
        <Space size={4}>
          <Button size="small" type="link" onClick={async () => setDetail(await api.get(`/requisitions/${r.id}`))}>详情</Button>
          {canIssue && r.status === 'APPLIED' &&
            <Button size="small" type="link" onClick={() => openIssue(r)}>
              {r.priority === 'URGENT' ? '紧急发料' : '发料核对'}
            </Button>}
          {canIssue && r.status === 'APPLIED' &&
            <Button size="small" type="link" danger
              onClick={() => { setRejectTarget(r); setRejectReason('') }}>驳回</Button>}
          {isCrew && r.status === 'ISSUED' && r.crewId === user.crewId &&
            <Popconfirm title="确认已收到全部材料？确认后进入现场消耗" onConfirm={() => confirm(r)}>
              <Button size="small" type="link">班组长确认</Button>
            </Popconfirm>}
        </Space>
      )
    }
  ]

  return (
    <Card title="领料单（申请 → 仓管核对发料 → 班组长确认）"
      extra={
        <Space>
          <Select allowClear placeholder="状态筛选" style={{ width: 130 }}
            value={statusFilter} onChange={setStatusFilter}
            options={Object.entries(REQ_STATUS).map(([k, v]) => ({ value: k, label: v }))} />
          <Button type="primary" onClick={() => { form.resetFields(); setApplyOpen(true) }}>＋ 申请领料</Button>
        </Space>
      }>
      <Table rowKey="id" loading={loading} columns={columns} dataSource={rows}
        size="small" scroll={{ x: 1200 }} pagination={{ pageSize: 15 }} />

      {/* 申请弹窗 */}
      <Modal title="领料申请" open={applyOpen} width={760} onOk={submitApply}
        onCancel={() => setApplyOpen(false)} destroyOnClose okText="提交申请">
        <Form form={form} layout="vertical"
          initialValues={{
            priority: 'NORMAL',
            crewId: isCrew ? user.crewId : undefined,
            items: [{}]
          }}>
          <Space style={{ display: 'flex' }}>
            {!isCrew && (
              <Form.Item name="crewId" label="班组" style={{ flex: 1 }} rules={[{ required: true }]}>
                <Select options={m.crews.map((c) => ({ value: c.id, label: `${c.name}（${c.trade}）` }))} />
              </Form.Item>
            )}
            <Form.Item name="buildingId" label="楼栋" style={{ flex: 1 }} rules={[{ required: true }]}>
              <Select options={m.buildings.map((b) => ({ value: b.id, label: b.name }))} />
            </Form.Item>
            <Form.Item name="processId" label="工序" style={{ flex: 1 }} rules={[{ required: true }]}>
              <Select options={m.processes.map((p) => ({ value: p.id, label: p.name }))} />
            </Form.Item>
          </Space>
          <Space style={{ display: 'flex' }}>
            <Form.Item name="workArea" label="施工区域（楼层/轴线）" style={{ flex: 1 }}>
              <Input placeholder="如 1#楼3层 A-C轴" />
            </Form.Item>
            <Form.Item name="drawingNo" label="图纸编号" style={{ flex: 1 }}>
              <Input placeholder="如 结施-03" />
            </Form.Item>
            <Form.Item name="priority" label="优先级" style={{ width: 130 }}>
              <Select options={Object.entries(PRIORITY).map(([k, v]) => ({ value: k, label: v }))} />
            </Form.Item>
          </Space>
          <Form.List name="items">
            {(fields, { add, remove }) => (
              <div>
                <Space style={{ marginBottom: 8 }}>
                  <Typography.Text strong>领用材料</Typography.Text>
                  <Button size="small" onClick={() => add({})}>＋ 添加材料</Button>
                </Space>
                {fields.map((f) => (
                  <Space key={f.key} align="baseline" style={{ display: 'flex', marginBottom: 8 }}>
                    <Form.Item name={[f.name, 'materialId']} rules={[{ required: true, message: '材料' }]}>
                      <Select showSearch optionFilterProp="label" style={{ width: 260 }}
                        placeholder="选择材料"
                        options={m.materials.map((x) => ({ value: x.id, label: `${x.name}（${x.unit}）` }))} />
                    </Form.Item>
                    <Form.Item name={[f.name, 'applyQty']} rules={[{ required: true, message: '数量' }]}>
                      <InputNumber min={0} precision={2} placeholder="申请量" addonAfter={
                        (() => {
                          const mid = form.getFieldValue(['items', f.name, 'materialId'])
                          return mid ? matUnit(mid) : ''
                        })()
                      } style={{ width: 170 }} />
                    </Form.Item>
                    <Form.Item name={[f.name, 'remark']}>
                      <Input placeholder="备注（可选）" style={{ width: 180 }} />
                    </Form.Item>
                    {fields.length > 1 && <Button type="link" danger onClick={() => remove(f.name)}>删除</Button>}
                  </Space>
                ))}
              </div>
            )}
          </Form.List>
          <Form.Item name="applyRemark" label="申请说明">
            <Input.TextArea rows={2} placeholder="如施工进度、紧急原因等" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 详情抽屉 */}
      <Drawer open={!!detail} onClose={() => setDetail(null)} width={640}
        title={detail ? `领料单 ${detail.reqNo}` : ''}>
        {detail && (
          <Space direction="vertical" style={{ width: '100%' }} size="middle">
            <Space wrap>
              <Tag color={REQ_STATUS_COLOR[detail.status]}>{REQ_STATUS[detail.status]}</Tag>
              {detail.priority === 'URGENT' && <Tag color="red">夜间紧急</Tag>}
              <span>{detail.buildingName} / {detail.processName} / {detail.crewName}</span>
            </Space>
            <div>施工区域：{detail.workArea} ｜ 图纸：{detail.drawingNo}</div>
            {detail.applyRemark && <Alert type="info" message={`申请说明：${detail.applyRemark}`} />}
            {detail.issueRemark && <Alert type="warning" message={`发料说明：${detail.issueRemark}`} />}
            {detail.rejectReason && <Alert type="error" message={`驳回原因：${detail.rejectReason}`} />}
            <Table size="small" rowKey="id" pagination={false} dataSource={detail.items}
              columns={[
                { title: '材料', dataIndex: 'materialName' },
                { title: '批次', dataIndex: 'batchNo', width: 160 },
                { title: '申请量', dataIndex: 'applyQty', width: 80, align: 'right' },
                { title: '实发量', dataIndex: 'issuedQty', width: 80, align: 'right',
                  render: (v) => v ?? '—' },
                { title: '单价', dataIndex: 'unitPrice', width: 90, align: 'right',
                  render: (v) => v == null ? '—' : money(v) },
                { title: '金额', dataIndex: 'lineCost', width: 100, align: 'right',
                  render: (v) => money(v) }
              ]} />
            {detail.status === 'ISSUED' && (
              isCrew && detail.crewId === user.crewId
                ? <Button type="primary" block onClick={() => confirm(detail)}>
                    我是班组长，确认领用（材料进入现场消耗）
                  </Button>
                : <Alert type="warning" showIcon message="等待班组长确认，当前材料尚未计入现场消耗" />
            )}
          </Space>
        )}
      </Drawer>

      {/* 发料核对弹窗 */}
      <Modal
        title={issueData ? `发料核对：${issueData.requisition.reqNo}${issueData.requisition.priority === 'URGENT' ? '（夜间紧急）' : ''}` : ''}
        open={!!issueData} width={980}
        onCancel={() => setIssueData(null)}
        onOk={submitIssue} okText="确认发料并扣减库存">
        {issueData && (
          <Space direction="vertical" style={{ width: '100%' }} size="middle">
            <Alert type="info" showIcon
              message={`${issueData.requisition.buildingName} / ${issueData.requisition.processName} / ${issueData.requisition.crewName}，区域：${issueData.requisition.workArea}`} />
            <Table size="small" rowKey="itemId" pagination={false} dataSource={issueData.items}
              columns={[
                { title: '材料', dataIndex: 'materialName' },
                { title: '申请', dataIndex: 'applyQty', width: 70, align: 'right' },
                {
                  title: '预算/已领/剩余', width: 190,
                  render: (_, i) => (
                    <div className="num" style={{ fontSize: 12 }}>
                      <div>预算 {num(i.budgetQty)}，已领 {num(i.alreadyIssuedQty)}</div>
                      <div className={i.overBudget ? 'positive' : 'muted'}>
                        剩余预算 {num(i.remainingBudgetQty)}{i.overBudget ? ' ⚠超预算' : ''}
                      </div>
                    </div>
                  )
                },
                { title: '现场库存', dataIndex: 'onHandQty', width: 90, align: 'right',
                  render: (v, i) => <span className={i.shortStock ? 'positive' : ''}>{num(v)} {i.unit}</span> },
                {
                  title: '发料批次（合格批次FIFO）', width: 230,
                  render: (i) => (
                    <Select size="small" style={{ width: 220 }}
                      value={issueForm.lines[i.itemId]?.batchId}
                      notFoundContent={<span className="positive">无合格库存（待检/不合格批次已冻结）</span>}
                      onChange={(x) => setIssueForm((p) => ({
                        ...p, lines: { ...p.lines, [i.itemId]: { ...p.lines[i.itemId], batchId: x } }
                      }))}
                      options={i.availableBatches.map((b) => ({
                        value: b.id,
                        label: `${b.batchNo}（在账 ${num(b.qtyOnHand)} ${i.unit}，${money(b.unitPrice)}元）`
                      }))} />
                  )
                },
                {
                  title: '实发量', width: 120,
                  render: (i) => (
                    <InputNumber size="small" min={0} max={Number(i.applyQty)} precision={2}
                      value={issueForm.lines[i.itemId]?.issuedQty}
                      onChange={(x) => setIssueForm((p) => ({
                        ...p, lines: { ...p.lines, [i.itemId]: { ...p.lines[i.itemId], issuedQty: x } }
                      }))}
                      addonAfter={i.unit} />
                  )
                }
              ]} />
            {issueData.items.some((i) => i.overBudget) && (
              <Checkbox checked={issueForm.allowOverBudget}
                onChange={(e) => setIssueForm((p) => ({ ...p, allowOverBudget: e.target.checked }))}>
                <span className="positive">超预算放行（需项目经理知悉，发料记录将保留超预算痕迹）</span>
              </Checkbox>
            )}
            <Input.TextArea rows={2} placeholder="发料备注（施工进度核对意见、紧急补发说明）"
              value={issueForm.issueRemark}
              onChange={(e) => setIssueForm((p) => ({ ...p, issueRemark: e.target.value }))} />
          </Space>
        )}
      </Modal>

      {/* 驳回弹窗 */}
      <Modal title={rejectTarget ? `驳回领料单 ${rejectTarget.reqNo}` : ''}
        open={!!rejectTarget} onOk={reject} onCancel={() => setRejectTarget(null)}
        okText="确认驳回" okButtonProps={{ danger: true }}>
        <Input.TextArea rows={3} value={rejectReason}
          onChange={(e) => setRejectReason(e.target.value)}
          placeholder="驳回原因（如：图纸不符、预算已用尽且无放行审批、现场进度未到工序等）" />
      </Modal>
    </Card>
  )
}
