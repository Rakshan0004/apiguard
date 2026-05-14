import { FormEvent, useEffect, useMemo, useState } from 'react'
import { Link, createFileRoute, useNavigate } from '@tanstack/react-router'
import { API_BASE_URL, type ApiKeySummary, type Plan, type RegisteredApi, request } from '~/lib/api'
import { clearToken, getToken } from '~/lib/auth'

export const Route = createFileRoute('/dashboard')({
  component: DashboardRoute,
})

function DashboardRoute() {
  const navigate = useNavigate()
  const [token, setToken] = useState<string | null>(() => getToken())

  const [apis, setApis] = useState<RegisteredApi[]>([])
  const [plansByApi, setPlansByApi] = useState<Record<string, Plan[]>>({})
  const [keys, setKeys] = useState<ApiKeySummary[]>([])

  const [apiName, setApiName] = useState('')
  const [targetUrl, setTargetUrl] = useState('')
  const [proxyPath, setProxyPath] = useState('')

  const [selectedApiId, setSelectedApiId] = useState('')
  const [planName, setPlanName] = useState('')
  const [rateLimitRpm, setRateLimitRpm] = useState(60)
  const [monthlyQuota, setMonthlyQuota] = useState(10000)
  const [webhookEnabled, setWebhookEnabled] = useState(false)

  const [selectedPlanId, setSelectedPlanId] = useState('')
  const [freshKey, setFreshKey] = useState<string | null>(null)

  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const selectedApiPlans = useMemo(() => {
    if (!selectedApiId) return []
    return plansByApi[selectedApiId] ?? []
  }, [plansByApi, selectedApiId])

  useEffect(() => {
    if (!token) {
      void navigate({ to: '/login' })
      return
    }
    void loadDashboardData(token)
  }, [token])

  async function loadDashboardData(activeToken: string) {
    setLoading(true)
    setError(null)
    try {
      const [apisRes, keysRes] = await Promise.all([
        request<RegisteredApi[]>('/api/v1/apis', {}, activeToken),
        request<ApiKeySummary[]>('/api/v1/keys', {}, activeToken),
      ])

      setApis(apisRes)
      setKeys(keysRes)

      const planEntries = await Promise.all(
        apisRes.map(async (api) => {
          const plans = await request<Plan[]>(`/api/v1/apis/${api.id}/plans`, {}, activeToken)
          return [api.id, plans] as const
        }),
      )
      setPlansByApi(Object.fromEntries(planEntries))

      if (apisRes.length && !selectedApiId) {
        const firstApiId = apisRes[0].id
        setSelectedApiId(firstApiId)
      }
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setLoading(false)
    }
  }

  async function createApi(e: FormEvent) {
    e.preventDefault()
    if (!token) return
    setLoading(true)
    setError(null)
    setMessage(null)
    try {
      await request<RegisteredApi>(
        '/api/v1/apis',
        {
          method: 'POST',
          body: JSON.stringify({ name: apiName, targetUrl, proxyPath }),
        },
        token,
      )
      setApiName('')
      setTargetUrl('')
      setProxyPath('')
      await loadDashboardData(token)
      setMessage('API registered.')
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setLoading(false)
    }
  }

  async function createPlan(e: FormEvent) {
    e.preventDefault()
    if (!token || !selectedApiId) return
    setLoading(true)
    setError(null)
    setMessage(null)
    try {
      const created = await request<Plan>(
        `/api/v1/apis/${selectedApiId}/plans`,
        {
          method: 'POST',
          body: JSON.stringify({
            name: planName,
            rateLimitRpm,
            monthlyQuota,
            webhookEnabled,
          }),
        },
        token,
      )

      setPlansByApi((prev) => ({
        ...prev,
        [selectedApiId]: [...(prev[selectedApiId] ?? []), created],
      }))

      setPlanName('')
      setRateLimitRpm(60)
      setMonthlyQuota(10000)
      setWebhookEnabled(false)
      setMessage('Plan created.')
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setLoading(false)
    }
  }

  async function createKey(e: FormEvent) {
    e.preventDefault()
    if (!token || !selectedApiId || !selectedPlanId) return
    setLoading(true)
    setError(null)
    setMessage(null)
    setFreshKey(null)
    try {
      const created = await request<{ apiKey: string; message: string }>(
        `/api/v1/keys/generate?apiId=${selectedApiId}&planId=${selectedPlanId}`,
        { method: 'POST' },
        token,
      )
      setFreshKey(created.apiKey)
      setMessage(created.message)
      await loadDashboardData(token)
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setLoading(false)
    }
  }

  function logout() {
    clearToken()
    setToken(null)
    setApis([])
    setPlansByApi({})
    setKeys([])
    setSelectedApiId('')
    setSelectedPlanId('')
    setFreshKey(null)
    setMessage('Logged out.')
    setError(null)
    void navigate({ to: '/login' })
  }

  return (
    <main className="shell">
      <header className="hero">
        <div>
          <h1>APIGuard Console</h1>
          <p className="subtitle">
            Connected to <code>{API_BASE_URL}</code>
          </p>
          <p className="subtitle">
            Need auth? <Link to="/login">Go to login</Link>
          </p>
        </div>
        <button className="danger" onClick={logout} type="button">
          Logout
        </button>
      </header>

      {message && <div className="status ok">{message}</div>}
      {error && <div className="status error">{error}</div>}

      <section className="grid two" style={{ marginBottom: 14 }}>
        <article className="panel">
          <h2 className="section-title">Register API</h2>
          <form className="grid" onSubmit={createApi}>
            <div>
              <label>Name</label>
              <input
                value={apiName}
                onChange={(e) => setApiName(e.target.value)}
                placeholder="Payments API"
                required
              />
            </div>
            <div>
              <label>Target URL</label>
              <input
                value={targetUrl}
                onChange={(e) => setTargetUrl(e.target.value)}
                placeholder="https://backend.company.com"
                required
              />
            </div>
            <div>
              <label>Proxy Path (lowercase, dash only)</label>
              <input
                value={proxyPath}
                onChange={(e) => setProxyPath(e.target.value)}
                placeholder="payments-api"
                required
              />
            </div>
            <button disabled={loading} type="submit">
              Register API
            </button>
          </form>
        </article>

        <article className="panel">
          <h2 className="section-title">Create Plan</h2>
          <form className="grid" onSubmit={createPlan}>
            <div>
              <label>API</label>
              <select
                value={selectedApiId}
                onChange={(e) => {
                  setSelectedApiId(e.target.value)
                  setSelectedPlanId('')
                }}
                required
              >
                <option value="">Select an API</option>
                {apis.map((api) => (
                  <option key={api.id} value={api.id}>
                    {api.name}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label>Plan Name</label>
              <input
                value={planName}
                onChange={(e) => setPlanName(e.target.value)}
                placeholder="Starter"
                required
              />
            </div>
            <div className="grid two">
              <div>
                <label>Rate Limit (RPM)</label>
                <input
                  type="number"
                  min={1}
                  value={rateLimitRpm}
                  onChange={(e) => setRateLimitRpm(Number(e.target.value))}
                  required
                />
              </div>
              <div>
                <label>Monthly Quota</label>
                <input
                  type="number"
                  min={1}
                  value={monthlyQuota}
                  onChange={(e) => setMonthlyQuota(Number(e.target.value))}
                  required
                />
              </div>
            </div>
            <label>
              <input
                type="checkbox"
                checked={webhookEnabled}
                onChange={(e) => setWebhookEnabled(e.target.checked)}
                style={{ width: 'auto', marginRight: 8 }}
              />
              Webhook enabled
            </label>
            <button disabled={loading || !selectedApiId} type="submit">
              Create Plan
            </button>
          </form>
        </article>
      </section>

      <section className="grid two" style={{ marginBottom: 14 }}>
        <article className="panel">
          <h2 className="section-title">Create API Key</h2>
          <form className="grid" onSubmit={createKey}>
            <div>
              <label>API</label>
              <select
                value={selectedApiId}
                onChange={(e) => {
                  setSelectedApiId(e.target.value)
                  setSelectedPlanId('')
                }}
                required
              >
                <option value="">Select API</option>
                {apis.map((api) => (
                  <option key={api.id} value={api.id}>
                    {api.name}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label>Plan</label>
              <select
                value={selectedPlanId}
                onChange={(e) => setSelectedPlanId(e.target.value)}
                required
              >
                <option value="">Select plan</option>
                {selectedApiPlans.map((plan) => (
                  <option key={plan.id} value={plan.id}>
                    {plan.name} ({plan.rateLimitRpm} rpm)
                  </option>
                ))}
              </select>
            </div>
            <button disabled={loading || !selectedApiId || !selectedPlanId} type="submit">
              Generate Key
            </button>
          </form>
          {freshKey && (
            <div className="status ok" style={{ marginTop: 12 }}>
              <strong>New key (copy now):</strong>
              <br />
              <code>{freshKey}</code>
            </div>
          )}
        </article>

        <article className="panel">
          <h2 className="section-title">Your APIs</h2>
          <div className="list">
            {apis.length === 0 && <p className="subtitle">No APIs yet.</p>}
            {apis.map((api) => (
              <div key={api.id} className="item">
                <strong>{api.name}</strong>
                <p>
                  Path: <code>/{api.proxyPath}</code>
                </p>
                <p>
                  Target: <code>{api.targetUrl}</code>
                </p>
                <p>Plans: {(plansByApi[api.id] ?? []).length}</p>
              </div>
            ))}
          </div>
        </article>
      </section>

      <section className="panel">
        <h2 className="section-title">Generated Keys</h2>
        <div className="list">
          {keys.length === 0 && <p className="subtitle">No keys generated yet.</p>}
          {keys.map((key) => (
            <div key={key.id} className="item">
              <strong>
                {key.apiName} · {key.planName}
              </strong>
              <p>
                Prefix: <code>{key.keyPrefix}</code>
              </p>
              <p>Status: {key.active ? 'Active' : 'Inactive'}</p>
              <p>Created: {new Date(key.createdAt).toLocaleString()}</p>
            </div>
          ))}
        </div>
      </section>
    </main>
  )
}
