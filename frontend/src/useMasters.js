import { useEffect, useState } from 'react'
import api from './api.js'

export function useMasters() {
  const [data, setData] = useState({
    crews: [], buildings: [], processes: [], suppliers: [], materials: [], budgets: []
  })
  const [loading, setLoading] = useState(true)

  const reload = async () => {
    setLoading(true)
    try {
      const [crews, buildings, processes, suppliers, materials, budgets] = await Promise.all([
        api.get('/masters/crews'),
        api.get('/masters/buildings'),
        api.get('/masters/processes'),
        api.get('/masters/suppliers'),
        api.get('/masters/materials'),
        api.get('/masters/budgets')
      ])
      setData({ crews, buildings, processes, suppliers, materials, budgets })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { reload() }, [])

  return { ...data, loading, reload }
}

export const nameOf = (list, id, key = 'name') =>
  list.find((x) => x.id === id)?.[key] ?? ''
