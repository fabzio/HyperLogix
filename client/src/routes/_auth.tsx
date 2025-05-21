import MainLayout from '@/layouts/MainLayout'
import { Outlet, createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/_auth')({
  component: RouteComponent,
})

function RouteComponent() {
  return (
    <MainLayout>
      <Outlet />
    </MainLayout>
  )
}
