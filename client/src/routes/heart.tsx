import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/heart')({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/heart"!</div>
}
