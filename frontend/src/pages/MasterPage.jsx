import { useState } from 'react'
import { Card, Tabs, Table, Button, Modal, Form, Input, Select, Space, Popconfirm, App } from 'antd'
import api from '../api.js'
import { useMasters } from '../useMasters.js'
import { MATERIAL_CATEGORIES } from '../dicts.js'

export default function MasterPage() {
  const m = useMasters()
  return (
    <Card title="基础数据维护">
      <Tabs
        items={[
          { key: 'crew', label: '班组', children: <CrewTab masters={m} /> },
          { key: 'building', label: '楼栋', children: <BuildingTab masters={m} /> },
          { key: 'process', label: '工序', children: <ProcessTab masters={m} /> },
          { key: 'supplier', label: '供应商', children: <SupplierTab masters={m} /> },
          { key: 'material', label: '材料目录', children: <MaterialTab masters={m} /> }
        ]} />
    </Card>
  )
}

function CrewTab({ masters }) {
  const { message } = App.useApp()
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()
  const save = async () => {
    const v = await form.validateFields()
    await api.post('/masters/crews', v)
    message.success('班组已保存')
    setOpen(false)
    form.resetFields()
    masters.reload()
  }
  return (
    <div>
      <Space style={{ marginBottom: 8 }}>
        <Button type="primary" onClick={() => { form.resetFields(); setOpen(true) }}>＋ 新增班组</Button>
      </Space>
      <Table rowKey="id" size="small" loading={masters.loading}
        dataSource={masters.crews} pagination={false}
        columns={[
          { title: '班组名称', dataIndex: 'name' },
          { title: '工种', dataIndex: 'trade' },
          { title: '班组长', dataIndex: 'leaderName' },
          { title: '电话', dataIndex: 'phone' },
          {
            title: '操作', width: 80,
            render: (_, r) => <Button size="small" type="link" onClick={() => { form.setFieldsValue(r); setOpen(true) }}>编辑</Button>
          }
        ]} />
      <Modal title="班组" open={open} onOk={save} onCancel={() => setOpen(false)} destroyOnClose>
        <Form form={form} layout="vertical">
          <Form.Item name="id" hidden><Input /></Form.Item>
          <Form.Item name="name" label="班组名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="trade" label="工种"><Input placeholder="钢筋/防水/镶贴/水电/砌筑/架子安全" /></Form.Item>
          <Form.Item name="leaderName" label="班组长"><Input /></Form.Item>
          <Form.Item name="phone" label="电话"><Input /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

function BuildingTab({ masters }) {
  const { message } = App.useApp()
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()
  const save = async () => {
    const v = await form.validateFields()
    await api.post('/masters/buildings', v)
    message.success('楼栋已保存')
    setOpen(false)
    form.resetFields()
    masters.reload()
  }
  return (
    <div>
      <Button type="primary" style={{ marginBottom: 8 }} onClick={() => { form.resetFields(); setOpen(true) }}>＋ 新增楼栋</Button>
      <Table rowKey="id" size="small" loading={masters.loading} dataSource={masters.buildings} pagination={false}
        columns={[
          { title: '楼栋编号', dataIndex: 'code' },
          { title: '楼栋名称', dataIndex: 'name' },
          { title: '所属项目', dataIndex: 'projectName' },
          { title: '操作', width: 80, render: (_, r) =>
            <Button size="small" type="link" onClick={() => { form.setFieldsValue(r); setOpen(true) }}>编辑</Button> }
        ]} />
      <Modal title="楼栋" open={open} onOk={save} onCancel={() => setOpen(false)} destroyOnClose>
        <Form form={form} layout="vertical">
          <Form.Item name="id" hidden><Input /></Form.Item>
          <Form.Item name="code" label="楼栋编号" rules={[{ required: true }]}><Input placeholder="如 1#、DX" /></Form.Item>
          <Form.Item name="name" label="楼栋名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="projectName" label="所属项目"><Input /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

function ProcessTab({ masters }) {
  const { message } = App.useApp()
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()
  const save = async () => {
    const v = await form.validateFields()
    await api.post('/masters/processes', v)
    message.success('工序已保存')
    setOpen(false)
    form.resetFields()
    masters.reload()
  }
  return (
    <div>
      <Button type="primary" style={{ marginBottom: 8 }} onClick={() => { form.resetFields(); setOpen(true) }}>＋ 新增工序</Button>
      <Table rowKey="id" size="small" loading={masters.loading} dataSource={masters.processes} pagination={false}
        columns={[
          { title: '工序编码', dataIndex: 'code' },
          { title: '工序名称', dataIndex: 'name' },
          { title: '操作', width: 80, render: (_, r) =>
            <Button size="small" type="link" onClick={() => { form.setFieldsValue(r); setOpen(true) }}>编辑</Button> }
        ]} />
      <Modal title="工序" open={open} onOk={save} onCancel={() => setOpen(false)} destroyOnClose>
        <Form form={form} layout="vertical">
          <Form.Item name="id" hidden><Input /></Form.Item>
          <Form.Item name="code" label="工序编码" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="name" label="工序名称" rules={[{ required: true }]}><Input /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

function SupplierTab({ masters }) {
  const { message } = App.useApp()
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()
  const save = async () => {
    const v = await form.validateFields()
    await api.post('/masters/suppliers', v)
    message.success('供应商已保存')
    setOpen(false)
    form.resetFields()
    masters.reload()
  }
  return (
    <div>
      <Button type="primary" style={{ marginBottom: 8 }} onClick={() => { form.resetFields(); setOpen(true) }}>＋ 新增供应商</Button>
      <Table rowKey="id" size="small" loading={masters.loading} dataSource={masters.suppliers} pagination={false}
        columns={[
          { title: '供应商名称', dataIndex: 'name' },
          { title: '联系人', dataIndex: 'contact' },
          { title: '电话', dataIndex: 'phone' },
          { title: '主营品类', dataIndex: 'category' },
          { title: '操作', width: 80, render: (_, r) =>
            <Button size="small" type="link" onClick={() => { form.setFieldsValue(r); setOpen(true) }}>编辑</Button> }
        ]} />
      <Modal title="供应商" open={open} onOk={save} onCancel={() => setOpen(false)} destroyOnClose>
        <Form form={form} layout="vertical">
          <Form.Item name="id" hidden><Input /></Form.Item>
          <Form.Item name="name" label="供应商名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="contact" label="联系人"><Input /></Form.Item>
          <Form.Item name="phone" label="电话"><Input /></Form.Item>
          <Form.Item name="category" label="主营品类"><Input /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

function MaterialTab({ masters }) {
  const { message } = App.useApp()
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()
  const save = async () => {
    const v = await form.validateFields()
    await api.post('/masters/materials', v)
    message.success('材料目录已保存')
    setOpen(false)
    form.resetFields()
    masters.reload()
  }
  return (
    <div>
      <Button type="primary" style={{ marginBottom: 8 }} onClick={() => { form.resetFields(); setOpen(true) }}>＋ 新增材料</Button>
      <Table rowKey="id" size="small" loading={masters.loading} dataSource={masters.materials} pagination={false}
        columns={[
          { title: '材料编码', dataIndex: 'code' },
          { title: '材料名称', dataIndex: 'name' },
          { title: '品类', dataIndex: 'category' },
          { title: '默认规格', dataIndex: 'spec' },
          { title: '单位', dataIndex: 'unit', width: 70 },
          { title: '操作', width: 80, render: (_, r) =>
            <Button size="small" type="link" onClick={() => { form.setFieldsValue(r); setOpen(true) }}>编辑</Button> }
        ]} />
      <Modal title="材料目录" open={open} onOk={save} onCancel={() => setOpen(false)} destroyOnClose>
        <Form form={form} layout="vertical">
          <Form.Item name="id" hidden><Input /></Form.Item>
          <Form.Item name="code" label="材料编码" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="name" label="材料名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="category" label="品类" rules={[{ required: true }]}>
            <Select options={MATERIAL_CATEGORIES.map((c) => ({ value: c, label: c }))} />
          </Form.Item>
          <Form.Item name="spec" label="默认规格"><Input /></Form.Item>
          <Form.Item name="unit" label="单位" rules={[{ required: true }]}>
            <Select options={['吨', '卷', '箱', '米', '套', '顶', '张', '个', '桶', '袋'].map((u) => ({ value: u, label: u }))} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
