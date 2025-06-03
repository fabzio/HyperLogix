import { cn } from '@/lib/utils'

interface SimulationHeaderProps {
  simulationTime: string | null
  isSimulationActive: boolean
}

export default function SimulationHeader({
  simulationTime,
  isSimulationActive,
}: SimulationHeaderProps) {
  const simulatedTimeDate = simulationTime ? new Date(simulationTime) : null

  return (
    <header>
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div
            className={cn(
              'w-2 h-2 rounded-full',
              isSimulationActive ? 'bg-green-500 animate-pulse' : 'bg-gray-400',
            )}
          />
          <p className="text-lg font-semibold">
            {isSimulationActive ? 'Simulación en curso' : 'Simulación detenida'}
          </p>
        </div>
        <div className="text-right">
          {isSimulationActive && simulatedTimeDate ? (
            <>
              <p className="text-xl font-mono font-bold text-primary">
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
