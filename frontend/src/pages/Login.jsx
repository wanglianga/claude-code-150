import { useState } from 'react'
import { Card, Form, Input, Button, Select, Typography, Space, Alert } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import api from '../api.js'
import { ROLES } from '../dicts.js'

const DEMO = [
  { username: 'pm', password: 'pm123', label: '项目经理' },
  { username: 'warehouse', password: 'wh123', label: '仓管员' },
  { username: 'wangqiang', password: 'wang123', label: '班组长（钢筋一班）' },
  { username: 'finance', password: 'fin123', label: '财务' },
  { username: 'budget', password: 'budget123', label: '预算员' },
  { username: 'admin', password: 'admin123', label: '管理员（全部功能）' }
]

export default function Login({ onLogin }) {
  const [loading, setLoading] = useState(false)
  const [err, setErr] = useState('')

  const submit = async (v) => {
    setLoading(true)
    setErr('')
    try {
      const res = await api.post('/auth/login', v)
      localStorage.setItem('token', res.token)
      localStorage.setItem('user', JSON.stringify(res.user))
      onLogin(res.user)
      location.hash = '#/'
    } catch (e) {
      setErr(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: 'linear-gradient(135deg,#0b3d91 0%,#1677ff 60%,#69b1ff 100%)'
    }}>
      <Card style={{ width: 440, boxShadow: '0 12px 40px rgba(0,0,0,.25)' }}>
        <Typography.Title level={3} style={{ textAlign: 'center', marginBottom: 4 }}>
          工地材料领用退库损耗
        </Typography.Title>
        <Typography.Title level={4} style={{ textAlign: 'center', marginTop: 0, color: '#1677ff' }}>
          与班组核算平台
        </Typography.Title>
        {err && <Alert type="error" message={err} style={{ marginBottom: 12 }} showIcon />}
        <Form onFinish={submit} size="large" initialValues={{ username: 'warehouse', password: 'wh123' }}>
          <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input prefix={<UserOutlined />} placeholder="用户名" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="密码" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={loading}>登 录</Button>
        </Form>
        <Typography.Text type="secondary" style={{ fontSize: 12 }}>演示账号（点击填充）：</Typography.Text>
        <Space wrap style={{ marginTop: 6 }}>
          {DEMO.map((d) => (
            <Button key={d.username} size="small" onClick={() => submit({ username: d.username, password: d.password })}>
              {d.label}
            </Button>
          ))}
        </Space>
      </Card>
    </div>
  )
}
