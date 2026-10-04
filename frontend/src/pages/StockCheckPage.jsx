import { useEffect, useState } from 'react'
import {
  Card, Table, Button, Tag, Modal, InputNumber, Select, Space, App, Typography, Alert, Input
} from 'antd'
import dayjs from 'dayjs'
import api from '../api.js'
import { RESPONSIBILITY, money } from '../dicts.js'

export default function StockCheckPage() {
  const { message } = App.useApp()
  const [list, setList] = useState([])
  const [detail, setDetail] = useState(null)
  const [drafts, setDrafts] = useState({}) // itemId -> {actualQty, responsibility, remark}
  const [loading, setLoading] = useState(false)

  const load = async () => {
    setLoading(true)
    try { setList(await api.get('/stock-checks')) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  const createCheck = async () => {
    const d = await api.post('/stock-checks', { remark: '例行盘点（系统自动快照全部批次账面数量）' })
    message.success('盘点单已创建，账面数量已快照')
    await load()
    openDetail(d.id)
  }

  const openDetail = async (id) => {
    const d = await api.get(`/stock-checks/${id}`)
    const draft = {}
    d.items.forEach((i) => { draft[i.id] = { actualQty: Number(i.actualQty), responsibility: i.responsibility, remark: i.remark || '' } })
    setDrafts(draft)
    setDetail(d)
  }

  const saveActuals = async () => {
    const lines = Object.entries(drafts).map(([id, v]) => ({
      itemId: Number(id), actualQty: v.actualQty, responsibility: v.responsibility, remark: v.remark
    }))
    await api.put(`/stock-checks/${detail.id}/actuals`, lines)
    message.success('实盘数据已保存')
    const d = await api.get(`/stock-checks/${detail.id}`)
    setDetail(d)
  }

  const confirmCheck = async () => {
    await saveActuals()
    const d = await api.post(`/stock-checks/${detail.id}/confirm`)
    message.success('盘点确认完成：盘盈/盘亏已调账，盘亏已生成损耗记录')
    setDetail(d)
    load()
  }

  const columns = [
    { title: '盘点单号', dataIndex: 'checkNo', width: 200 },
    { title: '盘点日期', dataIndex: 'checkDate', width: 120 },
    { title: '状态', dataIndex: 'status', width: 100,
      render: (v) => <Tag color={v === 'CONFIRMED' ? 'green' : 'gold'}>{v === 'CONFIRMED' ? '已确认' : '草稿'}</Tag> },
    { title: '明细行数', render: (_, r) => r.items?.length ?? 0, width: 90, align: 'right' },
    {
      title: '账实差异',
      render: (_, r) => {
        const diffs = (r.items || []).filter((i) => Number(i.diffQty) !== 0)
        if (!diffs.length) return <span className="muted">账实相符</span>
        return diffs.map((i) => `${i.materialName} ${i.diffQty > 0 ? '+' : ''}${i.diffQty}`).join('；')
      }
    },
    { title: '确认时间', dataIndex: 'confirmedAt', width: 170 },
    { title: '操作', width: 90, render: (_, r) => <Button size="small" type="link" onClick={() => openDetail(r.id)}>查看/录入</Button> }
  ]

  const itemColumns = [
    { title: '批次号', dataIndex: 'batchNo', width: 170 },
    { title: '材料', dataIndex: 'materialName' },
    { title: '单位', dataIndex: 'unit', width: 60 },
    { title: '账面数量', dataIndex: 'bookQty', width: 100, align: 'right' },
    {
      title: '实盘数量', dataIndex: 'actualQty', width: 130, align: 'right',
      render: (v, r) => detail?.status === 'DRAFT' ? (
        <InputNumber size="small" value={drafts[r.id]?.actualQty}
          onChange={(x) => setDrafts((p) => ({ ...p, [r.id]: { ...p[r.id], actualQty: x } }))} />
      ) : v
    },
    {
      title: '差异', width: 90, align: 'right',
      render: (_, r) => {
        const a = drafts[r.id]?.actualQty
        const diff = a === undefined ? 0 : Number(a) - Number(r.bookQty)
        return <span className={diff > 0 ? 'negative' : diff < 0 ? 'positive' : 'muted'}>
          {diff > 0 ? '+' : ''}{diff}
        </span>
      }
    },
    {
      title: '盘亏责任归属', width: 150,
      render: (_, r) => detail?.status === 'DRAFT' ? (
        <Select size="small" style={{ width: 140 }} value={drafts[r.id]?.responsibility}
          options={Object.entries(RESPONSIBILITY).map(([k, v]) => ({ value: k, label: v.replace(/（.*\)/, '') }))}
          onChange={(x) => setDrafts((p) => ({ ...p, [r.id]: { ...p[r.id], responsibility: x } }))} />
      ) : RESPONSIBILITY[r.responsibility]
    },
    {
      title: '备注',
      render: (_, r) => detail?.status === 'DRAFT' ? (
        <Input size="small" value={drafts[r.id]?.remark}
          onChange={(e) => setDrafts((p) => ({ ...p, [r.id]: { ...p[r.id], remark: e.target.value } }))} />
      ) : r.remark
    }
  ]

  return (
    <Card title="库存盘点（账实不符处理）" extra={<Button type="primary" onClick={createCheck}>＋ 发起盘点</Button>}>
      <Table rowKey="id" loading={loading} columns={columns} dataSource={list}
        size="small" pagination={false} expandable={{
          expandedRowRender: (r) => <Typography.Text type="secondary">备注：{r.remark || '—'}</Typography.Text>
        }} />

      <Modal
        title={`盘点单 ${detail?.checkNo || ''}`}
        open={!!detail} width={1100}
        onCancel={() => setDetail(null)}
        footer={detail?.status === 'DRAFT' ? [
          <Button key="save" onClick={saveActuals}>保存实盘</Button>,
          <Button key="ok" type="primary" danger onClick={confirmCheck}>确认盘点（自动调账并生成盘亏损耗）</Button>
        ] : [<Button key="c" type="primary" onClick={() => setDetail(null)}>关闭</Button>]}>
        <Alert type="info" style={{ marginBottom: 12 }} showIcon
          message="确认后：盘盈回补库存，盘亏调减库存并自动生成损耗记录（责任归属可在本单中指定，供应商责任自动标记为供应商质量索赔）。" />
        <Table rowKey="id" columns={itemColumns} dataSource={detail?.items || []}
          size="small" pagination={{ pageSize: 100 }} scroll={{ x: 900 }} />
      </Modal>
    </Card>
  )
}
