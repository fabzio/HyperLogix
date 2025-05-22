import DashboardFeature from '@/features/dashboard'
import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/_auth/')({
  component: Dashboard,
})

function Dashboard() {
  return <DashboardFeature />
}
