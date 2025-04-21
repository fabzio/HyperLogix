import { routeTree } from '@/routeTree.gen'
import type { QueryClient } from '@tanstack/react-query'
import { createRouter, RouterProvider } from '@tanstack/react-router'
import { render } from '@testing-library/react'

async function setup() {
  const router = createRouter({
    defaultPendingMinMs: 0,
    routeTree,
    context: {
      queryClient: null as unknown as QueryClient,
    },
  })
  const app = render(<RouterProvider router={router} />)
  return { router, app }
}
export default setup
