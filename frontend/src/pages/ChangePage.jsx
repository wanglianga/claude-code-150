import { useEffect, useState } from 'react'
import {
  Card, Table, Button, Tag, Space, Modal, Form, Select, InputNumber, Input, App, DatePicker
} from 'antd'
import dayjs from 'dayjs'
import api from '../api.js'
import { useMasters } from '../useMasters.js'
import { CHANGE_STATUS, CHANGE_TYPE, money } from '../dicts.js'

export default function ChangePage() {
  const { message } = App.useApp()
  const m = useMasters()
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()

  const load = async () => {
    setLoading(true)
    try { setRows(await api.get('/changes')) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  const submit = async () => {
    const v = await form.validateFields()
    await api.post('/changes', {
      buildingId: v.buildingId,
      processId: v.processId,
      materialId: v.materialId,
      crewId: v.crewId,
      type: v.type,
      changeQty: v.changeQty,
      claimAmount: v.claimAmount,
      changeDate: (v.changeDate || dayjs()).format('YYYY-MM-DD'),
      description: v.description
    })
    message.success('变更签证已登记，审批通过后进入变更索赔口径')
    setOpen(false)
    form.resetFields()
    load()
  }

  const setStatus = async (r, status) => {
    await api.post(`/changes/${r.id}/${status === 'APPROVED' ? 'approve' : 'reject'}`)
    message.success(status === 'APPROVED' ? '签证已批准，纳入变更索赔' : '签证已驳回')
    load()
  }

  const columns = [
    { title: '签证号', dataIndex: 'changeNo', width: 165 },
    { title: '日期', dataIndex: 'changeDate', width: 110 },
    { title: '类型', dataIndex: 'type', width: 90, render: (v) => CHANGE_TYPE[v] },
    { title: '楼栋', dataIndex: 'buildingId', width: 110,
      render: (id) => m.buildings.find((b) => b.id === id)?.name },
    { title: '工序', dataIndex: 'processId', width: 100,
      render: (id) => m.processes.find((p) => p.id === id)?.name },
    { title: '材料', dataIndex: 'materialId',
      render: (id) => m.materials.find((x) => x.id === id)?.name || '—' },
    { title: '班组', dataIndex: 'crewId', width: 110,
      render: (id) => m.crews.find((c) => c.id === id)?.name || '—' },
    { title: '增减量', dataIndex: 'changeQty', width: 90, align: 'right' },
    { title: '索赔金额', dataIndex: 'claimAmount', width: 110, align: 'right', render: money },
    { title: '状态', dataIndex: 'status', width: 90,
      render: (v) => {
        const color = v === 'APPROVED' ? 'green' : v === 'REJECTED' ? 'red' : 'gold'
        return <Tag color={color}>{CHANGE_STATUS[v]}</Tag>
      } },
    { title: '说明', dataIndex: 'description', ellipsis: true },
    { title: '操作', width: 150, fixed: 'right',
      render: (_, r) => r.status === 'DRAFT' ? (
        <Space size={0}>
          <Button size="small" type="link" onClick={() => setStatus(r, 'APPROVED')}>批准</Button>
          <Button size="small" type="link" danger onClick={() => setStatus(r, 'REJECTED')}>驳回</Button>
        </Space>
      ) : <span className="muted">已办结</span> }
  ]

  return (
    <Card title="变更签证（设计/施工变更 → 变更索赔依据）"
      extra={<Button type="primary" onClick={() => { form.resetFields(); setOpen(true) }}>＋ 登记变更签证</Button>}>
      <Table rowKey="id" loading={loading || m.loading} columns={columns} dataSource={rows}
        size="small" scroll={{ x: 1400 }} pagination={{ pageSize: 15 }} />

      <Modal title="变更签证登记" open={open} onOk={submit} onCancel={() => setOpen(false)} width={680} destroyOnClose>
        <Form form={form} layout="vertical" initialValues={{ type: 'DESIGN', changeDate: dayjs() }}>
          <Space style={{ display: 'flex' }}>
            <Form.Item name="buildingId" label="楼栋" style={{ flex: 1 }} rules={[{ required: true }]}>
              <Select options={m.buildings.map((b) => ({ value: b.id, label: b.name }))} />
            </Form.Item>
            <Form.Item name="processId" label="关联工序" style={{ flex: 1 }}>
              <Select allowClear options={m.processes.map((p) => ({ value: p.id, label: p.name }))} />
            </Form.Item>
            <Form.Item name="type" label="变更类型" style={{ width: 140 }} rules={[{ required: true }]}>
              <Select options={Object.entries(CHANGE_TYPE).map(([k, v]) => ({ value: k, label: v }))} />
            </Form.Item>
            <Form.Item name="changeDate" label="日期" style={{ width: 160 }}>
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
          </Space>
          <Space style={{ display: 'flex' }}>
            <Form.Item name="materialId" label="涉及材料" style={{ flex: 1 }}>
              <Select allowClear showSearch optionFilterProp="label"
                options={m.materials.map((x) => ({ value: x.id, label: x.name }))} />
            </Form.Item>
            <Form.Item name="crewId" label="涉及班组" style={{ flex: 1 }}>
              <Select allowClear options={m.crews.map((c) => ({ value: c.id, label: c.name }))} />
            </Form.Item>
            <Form.Item name="changeQty" label="增减数量" style={{ width: 140 }}>
              <InputNumber precision={2} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="claimAmount" label="索赔金额（元）" style={{ width: 160 }}
              rules={[{ required: true }]}>
              <InputNumber min={0} precision={2} style={{ width: '100%' }} />
            </Form.Item>
          </Space>
          <Form.Item name="description" label="变更内容与索赔说明" rules={[{ required: true }]}>
            <Input.TextArea rows={3} placeholder="如：设计变更卫生间排版方案调整，增加瓷砖15箱，索赔2700元" />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}
