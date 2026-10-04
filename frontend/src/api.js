import axios from 'axios'

const api = axios.create({ baseURL: '/api', timeout: 20000 })

api.interceptors.request.use((cfg) => {
  const token = localStorage.getItem('token')
  if (token) cfg.headers['X-Auth-Token'] = token
  return cfg
})

api.interceptors.response.use(
  (res) => res.data,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (!location.hash.startsWith('#/login')) location.hash = '#/login'
    }
    const msg = err.response?.data?.message || err.message || '请求失败'
    return Promise.reject(new Error(msg))
  }
)

export default api
