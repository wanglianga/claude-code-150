import { useEffect, useState } from 'react'
import {
  Card, Table, Button, Tag, Space, Modal, Form, Select, InputNumber, Input, App, Drawer, Alert, Popconfirm
} from 'antd'
import api from '../api.js'
import { useMasters } from '../useMasters.js'
import { RETURN_REASON, DISPOSITION, money } from '../dicts.js'

export default function ReturnPage({ user }) {
  const { message } = App.useApp()
  const m = useMasters()
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [open, setOpen] = useState(false)
  const [detail, setDetail] = useState(null)
  const [form] = Form.useForm()
  const isCrew = user.role === 'CREW_LEADER'
  const canConfirm = user.role === 'WAREHOUSE' || user.role === 'ADMIN'

  const load = async () => {
    setLoading(true)
    try {
      let data = await api.get('/returns')
      if (isCrew) data = data.filter((r) => r.crewId === user.crewId)
      setRows(data)
    } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  const submit = async () => {
    const v = await form.validateFields()
    await api.post('/returns', {
      crewId: isCrew ? user.crewId : v.crewId,
      buildingId: v.buildingId,
      processId: v.processId,
      materialId: v.materialId,
      batchId: v.batchId,
      qty: v.qty,
      reason: v.reason,
      disposition: v.disposition,
      remark: v.remark
    })
    message.success('退库单已登记，等待仓管确认；确认后可复用余料回补库存并冲减班组成本')
    setOpen(false)
    form.resetFields()
    load()
  }

  const confirm = async (r) => {
    await api.post(`/returns/${r.id}/confirm`)
    message.success(r.disposition === 'REUSE' ? '退库确认：余料已回补库存，班组成本已冲减' : '退库确认：报废计入班组责任成本')
    setDetail(null)
    load()
  }

  const columns = [
    { title: '退库单号', dataIndex: 'returnNo', width: 175 },
    { title: '状态', dataIndex: 'status', width: 90,
      render: (v) => <Tag color={v === 'CONFIRMED' ? 'green' : 'gold'}>{v === 'CONFIRMED' ? '已确认' : '待确认'}</Tag> },
    { title: '楼栋', dataIndex: 'buildingName', width: 100 },
    { title: '工序', dataIndex: 'processName', width: 100 },
    { title: '班组', dataIndex: 'crewName', width: 110 },
    { title: '材料', dataIndex: 'materialName' },
    { title: '数量', dataIndex: 'qty', width: 80, align: 'right',
      render: (v, r) => `${v} ${r.unit}` },
    { title: '金额', dataIndex: 'amount', width: 100, align: 'right', render: money },
    { title: '退库原因', dataIndex: 'reason', width: 110,
      render: (v) => RETURN_REASON[v] },
    { title: '处置', dataIndex: 'disposition', width: 110,
      render: (v) => <Tag color={v === 'REUSE' ? 'blue' : 'red'}>{DISPOSITION[v]}</Tag> },
    { title: '备注', dataIndex: 'remark', ellipsis: true },
    { title: '确认时间', dataIndex: 'confirmedAt', width: 165 },
    { title: '操作', width: 130, fixed: 'right',
      render: (_, r) => (
        <Space size={4}>
          <Button size="small" type="link" onClick={() => setDetail(r)}>详情</Button>
          {canConfirm && r.status === 'DRAFT' &&
            <Popconfirm title={`确认退库？${r.disposition === 'REUSE' ? '将回补库存并冲减班组成本' : '报废不回库存，计入班组责任成本'}`}
              onConfirm={() => confirm(r)}>
              <Button size="small" type="link">确认退库</Button>
            </Popconfirm>}
        </Space>
      ) }
  ]

  const materialIdWatch = Form.useWatch('materialId', form)
  const availableBatches = useBatchOptions(materialIdWatch)

  return (
    <Card title="退库单（临时追加 / 规格领错 / 施工变更）"
      extra={<Button type="primary" onClick={() => { form.resetFields(); setOpen(true) }}>＋ 登记退库</Button>}>
      <Table rowKey="id" loading={loading} columns={columns} dataSource={rows}
        size="small" scroll={{ x: 1400 }} pagination={{ pageSize: 15 }} />

      <Modal title="退库登记" open={open} onOk={submit} onCancel={() => setOpen(false)} width={640} destroyOnClose>
        <Form form={form} layout="vertical"
          initialValues={{ reason: 'SURPLUS', disposition: 'REUSE', crewId: isCrew ? user.crewId : undefined }}>
          <Space style={{ display: 'flex' }}>
            {!isCrew && (
              <Form.Item name="crewId" label="班组" style={{ flex: 1 }} rules={[{ required: true }]}>
                <Select options={m.crews.map((c) => ({ value: c.id, label: c.name }))} />
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
            <Form.Item name="materialId" label="材料" style={{ flex: 1 }} rules={[{ required: true }]}>
              <Select showSearch optionFilterProp="label"
                options={m.materials.map((x) => ({ value: x.id, label: `${x.name}（${x.unit}）` }))} />
            </Form.Item>
            <Form.Item name="batchId" label="原批次（可空）" style={{ flex: 1 }}>
              <Select allowClear options={availableBatches} />
            </Form.Item>
            <Form.Item name="qty" label="数量" style={{ width: 140 }} rules={[{ required: true }]}>
              <InputNumber min={0} precision={2} style={{ width: '100%' }} />
            </Form.Item>
          </Space>
          <Space style={{ display: 'flex' }}>
            <Form.Item name="reason" label="退库原因" style={{ flex: 1 }} rules={[{ required: true }]}>
              <Select options={Object.entries(RETURN_REASON).map(([k, v]) => ({ value: k, label: v }))} />
            </Form.Item>
            <Form.Item name="disposition" label="处置方式" style={{ flex: 1 }} rules={[{ required: true }]}>
              <Select options={Object.entries(DISPOSITION).map(([k, v]) => ({ value: k, label: v }))} />
            </Form.Item>
          </Space>
          <Form.Item name="remark" label="情况说明">
            <Input.TextArea rows={3} placeholder="如：领成600×600规格开箱后无法退货；施工变更防水范围调整余料退回等" />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer open={!!detail} onClose={() => setDetail(null)} width={480} title="退库单详情">
        {detail && (
          <Space direction="vertical" style={{ width: '100%' }}>
            <Space wrap>
              <Tag color={detail.status === 'CONFIRMED' ? 'green' : 'gold'}>
                {detail.status === 'CONFIRMED' ? '已确认' : '待确认'}
              </Tag>
              <Tag color={detail.disposition === 'REUSE' ? 'blue' : 'red'}>{DISPOSITION[detail.disposition]}</Tag>
            </Space>
            <div>{detail.buildingName} / {detail.processName} / {detail.crewName}</div>
            <div>材料：{detail.materialName}，数量：{detail.qty} {detail.unit}，金额：{money(detail.amount)} 元</div>
            <div>原因：{RETURN_REASON[detail.reason]}</div>
            {detail.disposition === 'REUSE'
              ? <Alert type="success" showIcon message="可复用余料：确认后回补批次库存，并在月度核算中冲减班组材料成本" />
              : <Alert type="error" showIcon message="报废：不回补库存，金额计入班组责任损耗成本" />}
            <div>说明：{detail.remark}</div>
            {canConfirm && detail.status === 'DRAFT' &&
              <Button type="primary" block onClick={() => confirm(detail)}>仓管确认退库</Button>}
          </Space>
        )}
      </Drawer>
    </Card>
  )
}

function useBatchOptions(materialId) {
  const [opts, setOpts] = useState([])
  useEffect(() => {
    if (!materialId) { setOpts([]); return }
    api.get('/inventory/available', { params: { materialId } })
      .then((rows) => setOpts(rows.map((b) => ({ value: b.id, label: `${b.batchNo}（在账 ${b.qtyOnHand}）` }))))
      .catch(() => setOpts([]))
  }, [materialId])
  return opts
}
