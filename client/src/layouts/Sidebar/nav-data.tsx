import Codepen from '@/components/icons/Codepen'
import type { LinkProps } from '@tanstack/react-router'
import {
  BookOpenText,
  ChartCandlestick,
  Database,
  Fuel,
  Monitor,
  Truck,
} from 'lucide-react'

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
      title: 'Operación Diaria',
      url: '/',
      icon: <Monitor size={16} />,
    },
    {
      title: 'Simulación',
      url: '/simulacion',
      icon: <Codepen />,
    },
    {
      title: 'Camiones',
      url: '/trucks',
      icon: <Truck size={16} />,
    },
    {
      title: 'Estaciones',
      url: '/stations',
      icon: <Fuel size={16} />,
    },

    {
      title: 'Benchmark',
      url: '/benchmark',
      icon: <ChartCandlestick size={16} />,
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
