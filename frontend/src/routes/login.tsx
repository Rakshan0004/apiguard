import { FormEvent, useEffect, useState } from 'react'
import { Link, createFileRoute, useNavigate, useSearch } from '@tanstack/react-router'
import { type AuthResponse, request } from '~/lib/api'
import { getToken, setToken } from '~/lib/auth'

export const Route = createFileRoute('/login')({
  validateSearch: (s: Record<string, unknown>) => ({
    registered: s.registered === 'true',
  }),
  component: LoginPage,
})

function LoginPage() {
  const navigate = useNavigate()
  const search = useSearch({ from: '/login' })
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [showPass, setShowPass] = useState(false)

  useEffect(() => {
    if (getToken()) void navigate({ to: '/dashboard' })
  }, [navigate])

  async function handleLogin(e: FormEvent) {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const data = await request<AuthResponse>('/api/v1/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      })
      setToken(data.token)
      localStorage.setItem('apiguard_email', email)
      void navigate({ to: '/dashboard' })
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        {/* Logo */}
        <div className="auth-logo">
          <div className="auth-logo-icon">A</div>
          <span className="auth-logo-name">API<span>Guard</span></span>
        </div>

        {/* Registered Banner */}
        {search.registered && (
          <div style={{
            background: 'rgba(52,211,153,0.1)',
            border: '1px solid rgba(52,211,153,0.25)',
            borderRadius: 8,
            padding: '10px 14px',
            marginBottom: 20,
            fontSize: '0.87rem',
            color: 'var(--green)',
            display: 'flex',
            alignItems: 'center',
            gap: 8,
          }}>
            <span style={{ fontWeight: 700 }}>✓</span>
            Account created! Sign in to continue.
          </div>
        )}

        <h1 className="auth-title">Welcome back</h1>
        <p className="auth-subtitle">Sign in to your APIGuard console</p>

        <form onSubmit={handleLogin} className="form-grid" style={{ marginTop: 24 }}>
          <div className="form-group">
            <label htmlFor="login-email">Email address</label>
            <input
              id="login-email"
              type="email"
              value={email}
              required
              autoComplete="email"
              onChange={e => setEmail(e.target.value)}
              placeholder="you@company.com"
            />
          </div>

          <div className="form-group">
            <label htmlFor="login-password">Password</label>
            <div style={{ position: 'relative' }}>
              <input
                id="login-password"
                type={showPass ? 'text' : 'password'}
                value={password}
                required
                minLength={6}
                autoComplete="current-password"
                onChange={e => setPassword(e.target.value)}
                placeholder="••••••••"
                style={{ paddingRight: 44 }}
              />
              <button
                type="button"
                onClick={() => setShowPass(v => !v)}
                style={{
                  position: 'absolute', right: 12, top: '50%',
                  transform: 'translateY(-50%)',
                  background: 'transparent', border: 'none',
                  color: 'var(--text-3)', cursor: 'pointer', fontSize: '0.85rem', padding: 0,
                }}
              >
                {showPass ? '🙈' : '👁'}
              </button>
            </div>
          </div>

          {error && (
            <div style={{
              background: 'var(--red-bg)',
              border: '1px solid rgba(248,113,113,0.25)',
              borderRadius: 8,
              padding: '10px 14px',
              fontSize: '0.85rem',
              color: 'var(--red)',
            }}>
              {error}
            </div>
          )}

          <button className="btn btn-primary btn-full btn-lg" type="submit" disabled={loading}>
            {loading ? <><span className="spinner" /> Signing in…</> : 'Sign in'}
          </button>
        </form>

        <div className="auth-footer" style={{ marginTop: 24 }}>
          Don't have an account?{' '}
          <Link to="/register">Create one free</Link>
        </div>
      </div>
    </div>
  )
}
