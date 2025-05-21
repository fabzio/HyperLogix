import type { LinkProps } from '@tanstack/react-router'
import { BookOpenText, ChartCandlestick, Codepen, Database } from 'lucide-react'

type NavData = {
  navMain: {
    title: string
    url: LinkProps['to']
    icon: React.ReactNode
    isActive?: boolean
  }[]
}
export const data: NavData = {
  navMain: [
    {
      title: 'Benchmark',
      url: '/benchmark',
      icon: <ChartCandlestick size={16} />,
    },
    {
      title: 'Simulación',
      url: '/simulacion',
      icon: <Codepen size={16} />,
    },
    {
      title: 'Inicio',
      url: '/start',
      icon: <Database size={16} />,
    },
    {
      title: 'Tablas',
      url: '/demo/table',
      icon: <BookOpenText size={16} />,
    },
    {
      title: 'Tanstack Query',
      url: '/demo/tanstack-query',
      icon: <BookOpenText size={16} />,
    },
  ],
}
