import { createRouter } from '@tanstack/react-router'
import { routeTree } from './routeTree.gen'

export function getRouter() {
  const router = createRouter({
    routeTree,
    defaultPreload: 'intent',
    defaultErrorComponent: ({ error }) => (
      <main className="shell center">
        <h1>Something went wrong</h1>
        <pre>{error.message}</pre>
      </main>
    ),
    defaultNotFoundComponent: () => (
      <main className="shell center">
        <h1>Not found</h1>
      </main>
    ),
    scrollRestoration: true,
  })

  return router
}

declare module '@tanstack/react-router' {
  interface Register {
    router: ReturnType<typeof getRouter>
  }
}
