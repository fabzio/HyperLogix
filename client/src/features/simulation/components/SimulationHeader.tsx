'use client'

import { cn } from '@/lib/utils'
import { PauseCircle, PlayCircle } from 'lucide-react'
import { useEffect, useState } from 'react'

interface SimulationHeaderProps {
  simulationTime: string | null
  isSimulationActive: boolean
  acceleration: number
  startTime: Date | null
}

function formatDuration(ms: number) {
  const totalSeconds = Math.floor(ms / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  return `${hours.toString().padStart(2, '0')}:${minutes
    .toString()
    .padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
}

export default function SimulationHeader({
  simulationTime,
  isSimulationActive,
  acceleration,
  startTime,
}: SimulationHeaderProps) {
  const simulatedTimeDate = simulationTime ? new Date(simulationTime) : null
  const [duration, setDuration] = useState<string>('00:00:00')

  useEffect(() => {
    if (!isSimulationActive || !startTime) return

    const updateDuration = () => {
      const now = new Date()
      const diff = now.getTime() - startTime.getTime()
      setDuration(formatDuration(diff))
    }

    updateDuration() // set immediately
    const interval = setInterval(updateDuration, 1000) // update every second

    return () => clearInterval(interval) // clean up
  }, [isSimulationActive, startTime])

  return (
    <header className="px-2">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div
            className={cn(
              'w-2 h-2 rounded-full',
              isSimulationActive ? 'bg-green-500 animate-pulse' : 'bg-gray-400',
            )}
          />
          {isSimulationActive ? (
            <PauseCircle className="w-7 h-7 text-primary" />
          ) : (
            <PlayCircle className="w-7 h-7 text-primary" />
          )}
          <p className="text-base font-semibold">
            {isSimulationActive ? 'Simulación en curso' : 'Simulación detenida'}
          </p>
          <p>{isSimulationActive ? `${acceleration.toFixed(1)}x` : ''}</p>
          <p>
            {isSimulationActive && startTime
              ? `Tiempo simulando: ${duration}`
              : ''}
          </p>
        </div>
        <div className="text-right">
          {isSimulationActive && simulatedTimeDate ? (
            <>
              <p className="text-md font-mono font-bold text-primary">
                {simulatedTimeDate.toLocaleDateString('es-ES', {
                  weekday: 'long',
                  year: 'numeric',
                  month: 'long',
                  day: 'numeric',
                })}
              </p>
              <p className="text-xl font-mono text-muted-foreground">
                {simulatedTimeDate.toLocaleTimeString('es-ES', {
                  hour: '2-digit',
                  minute: '2-digit',
                  second: '2-digit',
                })}
              </p>
            </>
          ) : (
            <p className="text-lg text-muted-foreground">
              No hay datos de simulación
            </p>
          )}
        </div>
      </div>
    </header>
  )
}
