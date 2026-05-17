import { FormEvent, useEffect, useState } from 'react'
import { Link, createFileRoute, useNavigate } from '@tanstack/react-router'
import { type AuthResponse, request } from '~/lib/api'
import { getToken } from '~/lib/auth'

export const Route = createFileRoute('/register')({
  component: RegisterPage,
})

function RegisterPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [showPass, setShowPass] = useState(false)

  useEffect(() => {
    if (getToken()) void navigate({ to: '/dashboard' })
  }, [navigate])

  async function handleRegister(e: FormEvent) {
    e.preventDefault()
    if (password !== confirm) {
      setError('Passwords do not match.')
      return
    }
    setLoading(true)
    setError(null)
    try {
      await request<AuthResponse>('/api/v1/auth/register', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      })
      // ✅ FIX: go to /login with success flag, NOT straight to dashboard
      void navigate({ to: '/login', search: { registered: 'true' } })
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setLoading(false)
    }
  }

  const strength = password.length === 0 ? 0
    : password.length < 6 ? 1
    : password.length < 10 ? 2
    : /[A-Z]/.test(password) && /[0-9]/.test(password) ? 4
    : 3

  const strengthColor = ['transparent','var(--red)','var(--amber)','var(--accent)','var(--green)'][strength]
  const strengthLabel = ['','Weak','Fair','Good','Strong'][strength]

  return (
    <div className="auth-page">
      <div className="auth-card">
        {/* Logo */}
        <div className="auth-logo">
          <div className="auth-logo-icon">A</div>
          <span className="auth-logo-name">API<span>Guard</span></span>
        </div>

        <h1 className="auth-title">Create your account</h1>
        <p className="auth-subtitle">Start managing your APIs in minutes — no credit card required</p>

        <form onSubmit={handleRegister} className="form-grid" style={{ marginTop: 24 }}>
          <div className="form-group">
            <label htmlFor="reg-email">Work email</label>
            <input
              id="reg-email"
              type="email"
              value={email}
              required
              autoComplete="email"
              onChange={e => setEmail(e.target.value)}
              placeholder="you@company.com"
            />
          </div>

          <div className="form-group">
            <label htmlFor="reg-password">Password</label>
            <div style={{ position: 'relative' }}>
              <input
                id="reg-password"
                type={showPass ? 'text' : 'password'}
                value={password}
                required
                minLength={6}
                autoComplete="new-password"
                onChange={e => setPassword(e.target.value)}
                placeholder="Min. 6 characters"
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
            {/* Strength meter */}
            {password.length > 0 && (
              <div style={{ marginTop: 6 }}>
                <div style={{ display: 'flex', gap: 4, marginBottom: 4 }}>
                  {[1,2,3,4].map(i => (
                    <div key={i} style={{
                      flex: 1, height: 3, borderRadius: 99,
                      background: i <= strength ? strengthColor : 'var(--surface-3)',
                      transition: 'background 0.3s',
                    }} />
                  ))}
                </div>
                <span style={{ fontSize: '0.75rem', color: strengthColor }}>{strengthLabel}</span>
              </div>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="reg-confirm">Confirm password</label>
            <input
              id="reg-confirm"
              type={showPass ? 'text' : 'password'}
              value={confirm}
              required
              minLength={6}
              autoComplete="new-password"
              onChange={e => setConfirm(e.target.value)}
              placeholder="Re-enter password"
              style={{ borderColor: confirm && confirm !== password ? 'rgba(248,113,113,0.5)' : '' }}
            />
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
            {loading ? <><span className="spinner" /> Creating account…</> : 'Create free account'}
          </button>

          <p style={{ fontSize: '0.78rem', color: 'var(--text-3)', textAlign: 'center' }}>
            By creating an account you agree to our Terms of Service and Privacy Policy.
          </p>
        </form>

        <div className="auth-footer">
          Already have an account?{' '}
          <Link to="/login">Sign in</Link>
        </div>
      </div>
    </div>
  )
}
