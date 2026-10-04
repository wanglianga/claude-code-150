import { useEffect, useState } from 'react'
import { Card, DatePicker, Table, Row, Col, Statistic, Tag, Space, Empty } from 'antd'
import dayjs from 'dayjs'
import {
  PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend, BarChart, Bar,
  XAxis, YAxis, CartesianGrid
} from 'recharts'
import api from '../api.js'
import { LOSS_REASON, RESPONSIBILITY_SHORT, money } from '../dicts.js'

const COLORS = {
  DESIGN_CHANGE: '#722ed1',
  CONSTRUCTION_ERROR: '#cf1322',
  POOR_STORAGE: '#fa8c16',
  SUPPLIER_QUALITY: '#eb2f96',
  NORMAL: '#8c8c8c'
}

export default function LossReviewPage() {
  const [period, setPeriod] = useState(dayjs().format('YYYY-MM'))
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setLoading(true)
    api.get('/analytics/loss-review', { params: { period } })
      .then(setData)
      .finally(() => setLoading(false))
  }, [period])

  const byReason = data?.byReason || []
  const details = data?.details || []
  const pieData = byReason.map((r) => ({ name: LOSS_REASON[r.lossReason], value: Number(r.amount), key: r.lossReason }))
  const barData = byReason.map((r) => ({
    name: LOSS_REASON[r.lossReason],
    金额: Number(r.amount),
    笔数: r.count
  }))

  const detailCols = [
    { title: '损耗单号', dataIndex: 'lossNo', width: 170 },
    { title: '日期', dataIndex: 'lossDate', width: 110 },
    { title: '材料', dataIndex: 'materialName' },
    { title: '数量', dataIndex: 'qty', width: 90, align: 'right' },
    { title: '金额', dataIndex: 'amount', width: 100, align: 'right', render: money },
    { title: '损耗来源', dataIndex: 'lossReason', width: 110,
      render: (v) => <Tag color={COLORS[v]}>{LOSS_REASON[v]}</Tag> },
    { title: '责任承担', dataIndex: 'responsibility', width: 90,
      render: (v) => <Tag>{RESPONSIBILITY_SHORT[v]}</Tag> },
    { title: '班组', dataIndex: 'crewName', width: 110 },
    { title: '供应商', dataIndex: 'supplierName', width: 160 },
    { title: '复盘说明', dataIndex: 'remark', ellipsis: true }
  ]

  return (
    <Card title={`材料员月底损耗复盘（${period}）：损耗来自设计变更 / 施工错误 / 保管不当 / 供应商质量`}
      extra={<DatePicker picker="month" value={dayjs(period)}
        onChange={(d) => d && setPeriod(d.format('YYYY-MM'))} />}>
      <Row gutter={12} style={{ marginBottom: 12 }}>
        <Col span={5}><Card size="small"><Statistic title="损耗总金额" loading={loading}
          value={Number(data?.totalLoss || 0)} precision={2} prefix="¥"
          valueStyle={{ color: '#cf1322' }} /></Card></Col>
        <Col span={5}><Card size="small"><Statistic title="损耗笔数" value={details.length} /></Card></Col>
        <Col span={7}><Card size="small">
          <div className="muted" style={{ fontSize: 12, marginBottom: 4 }}>管理建议</div>
          <Space direction="vertical" size={2}>
            {byReason.slice(0, 2).map((r) => (
              <span key={r.lossReason} style={{ fontSize: 13 }}>
                <Tag color={COLORS[r.lossReason]}>{LOSS_REASON[r.lossReason]}</Tag>
                占比 {Number(data.totalLoss) > 0
                  ? (Number(r.amount) * 100 / Number(data.totalLoss)).toFixed(1) : 0}%，应重点管控
              </span>
            ))}
          </Space>
        </Card></Col>
        <Col span={7}><Card size="small">
          <div className="muted" style={{ fontSize: 12, marginBottom: 4 }}>责任去向</div>
          <Space direction="vertical" size={2}>
            {['CREW', 'SUPPLIER', 'PROJECT'].map((resp) => {
              const amt = details.filter((d) => d.responsibility === resp)
                .reduce((s, d) => s + Number(d.amount), 0)
              return <span key={resp} style={{ fontSize: 13 }}>
                <Tag>{RESPONSIBILITY_SHORT[resp]}承担</Tag> {money(amt)} 元
              </span>
            })}
          </Space>
        </Card></Col>
      </Row>

      {pieData.length === 0 && !loading ? <Empty description="本月暂无损耗记录" /> : (
        <Row gutter={12}>
          <Col span={10}>
            <Card size="small" title="损耗金额构成（按原因）">
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie data={pieData} dataKey="value" nameKey="name" cx="50%" cy="50%"
                    outerRadius={100} label={(d) => `${d.name} ${money(d.value)}`}>
                    {pieData.map((d) => <Cell key={d.key} fill={COLORS[d.key]} />)}
                  </Pie>
                  <Tooltip formatter={(v) => money(v)} />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            </Card>
          </Col>
          <Col span={14}>
            <Card size="small" title="各原因损耗金额对比">
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={barData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" fontSize={11} />
                  <YAxis />
                  <Tooltip formatter={(v, name) => [money(v), name]} />
                  <Bar dataKey="金额" radius={[4, 4, 0, 0]}>
                    {barData.map((d, i) => {
                      const key = Object.keys(LOSS_REASON).find((k) => LOSS_REASON[k] === d.name)
                      return <Cell key={i} fill={COLORS[key]} />
                    })}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </Card>
          </Col>
        </Row>
      )}

      <Card size="small" style={{ marginTop: 12 }} title="损耗明细与责任追溯">
        <Table rowKey="lossNo" loading={loading} size="small" columns={detailCols}
          dataSource={details} pagination={{ pageSize: 15 }} scroll={{ x: 1200 }} />
      </Card>
    </Card>
  )
}
