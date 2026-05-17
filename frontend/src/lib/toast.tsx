import React, { createContext, useCallback, useContext, useRef, useState } from 'react'

type ToastType = 'success' | 'error' | 'info'
interface Toast { id: number; message: string; type: ToastType }

interface ToastCtx { toast: (msg: string, type?: ToastType) => void }
const Ctx = createContext<ToastCtx>({ toast: () => {} })

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])
  const counter = useRef(0)

  const toast = useCallback((message: string, type: ToastType = 'info') => {
    const id = ++counter.current
    setToasts(p => [...p, { id, message, type }])
    setTimeout(() => setToasts(p => p.filter(t => t.id !== id)), 4000)
  }, [])

  const icons: Record<ToastType, string> = { success: '✓', error: '✕', info: 'ℹ' }

  return (
    <Ctx.Provider value={{ toast }}>
      {children}
      <div className="toast-container">
        {toasts.map(t => (
          <div key={t.id} className={`toast toast-${t.type}`}>
            <span style={{ fontWeight: 700 }}>{icons[t.type]}</span>
            {t.message}
          </div>
        ))}
      </div>
    </Ctx.Provider>
  )
}

export const useToast = () => useContext(Ctx)
