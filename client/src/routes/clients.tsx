import Clients from '@/views/Clients'
import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/clients')({
  component: Clients,
})
