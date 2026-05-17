import React, { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { useNavigate } from '@tanstack/react-router'
import { type ApiKeySummary, type Plan, type RegisteredApi, request } from '~/lib/api'
import { getToken } from '~/lib/auth'
import { useToast } from '~/lib/toast'

interface DashboardData {
  token: string | null
  apis: RegisteredApi[]
  plansByApi: Record<string, Plan[]>
  keys: ApiKeySummary[]
  loading: boolean
  reload: () => Promise<void>
}

const Ctx = createContext<DashboardData>({
  token: null, apis: [], plansByApi: {}, keys: [], loading: false, reload: async () => {},
})

export function DashboardProvider({ children }: { children: React.ReactNode }) {
  const navigate = useNavigate()
  const { toast } = useToast()
  const [token] = useState<string | null>(() => getToken())
  const [apis, setApis] = useState<RegisteredApi[]>([])
  const [plansByApi, setPlansByApi] = useState<Record<string, Plan[]>>({})
  const [keys, setKeys] = useState<ApiKeySummary[]>([])
  const [loading, setLoading] = useState(false)

  const reload = useCallback(async () => {
    if (!token) { void navigate({ to: '/login' }); return }
    setLoading(true)
    try {
      const [apisRes, keysRes] = await Promise.all([
        request<RegisteredApi[]>('/api/v1/apis', {}, token),
        request<ApiKeySummary[]>('/api/v1/keys', {}, token),
      ])
      setApis(apisRes)
      setKeys(keysRes)
      const planEntries = await Promise.all(
        apisRes.map(async api => {
          const plans = await request<Plan[]>(`/api/v1/apis/${api.id}/plans`, {}, token)
          return [api.id, plans] as const
        })
      )
      setPlansByApi(Object.fromEntries(planEntries))
    } catch (e) {
      toast((e as Error).message, 'error')
    } finally {
      setLoading(false)
    }
  }, [token, navigate, toast])

  useEffect(() => { void reload() }, [])

  return (
    <Ctx.Provider value={{ token, apis, plansByApi, keys, loading, reload }}>
      {children}
    </Ctx.Provider>
  )
}

export const useDashboard = () => useContext(Ctx)
