import Benchmark from '@/features/benchmark'
import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/benchmark')({
  component: Benchmark,
})
