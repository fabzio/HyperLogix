import { Outlet, createRootRouteWithContext } from '@tanstack/react-router'
import { TanStackRouterDevtools } from '@tanstack/react-router-devtools'

import TanstackQueryLayout from '../integrations/tanstack-query/layout'

import LoadingBarProvider from '@/layouts/LoadingBarProvider'
import type { QueryClient } from '@tanstack/react-query'
import { Toaster } from 'sonner'

interface MyRouterContext {
  queryClient: QueryClient
}

export const Route = createRootRouteWithContext<MyRouterContext>()({
  component: () => (
    <>
      <LoadingBarProvider>
        <Outlet />
        <Toaster />
        {/* <TanStackRouterDevtools />*/}
        {/*<TanstackQueryLayout />*/}
      </LoadingBarProvider>
    </>
  ),
})
