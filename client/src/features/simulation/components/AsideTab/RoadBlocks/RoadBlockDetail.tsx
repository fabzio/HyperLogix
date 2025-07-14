import { useSearch } from '@tanstack/react-router'
import { useWatchSimulation } from '@/features/simulation/hooks/useSimulation.tsx'
import { useNavigate } from '@tanstack/react-router'
import { X } from 'lucide-react'

export function RoadblockDetail() {
  const { roadblockStart } = useSearch({ from: '/_auth/simulacion' })
  const navigate = useNavigate({ from: '/simulacion' })
  const { plgNetwork: network } = useWatchSimulation()

  const roadblock = network?.roadblocks?.find(
    (rb) => rb.start === roadblockStart,
  )

  if (!roadblock) return null

  const startDate = new Date(roadblock.start)
  const endDate = new Date(roadblock.end)

  return (
    <div className="w-80 h-full border-l border-zinc-800 bg-zinc-900 text-white p-4 flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold">Bloqueo de ruta</h2>
        <button
          type="button"
          onClick={() => navigate({ search: {} })}
          className="hover:text-red-400"
        >
          <X size={20} />
        </button>
      </div>

      <div className="space-y-2 text-sm">
        <div>
          <span className="text-zinc-400">Fecha:</span>{' '}
          {startDate.toLocaleDateString()}
        </div>
        <div>
          <span className="text-zinc-400">Hora inicio:</span>{' '}
          {startDate.toLocaleTimeString()}
        </div>
        <div>
          <span className="text-zinc-400">Hora fin:</span>{' '}
          {endDate.toLocaleTimeString()}
        </div>
        <div>
          <span className="text-zinc-400">Nodos bloqueados:</span>{' '}
          {roadblock.blockedNodes?.length ?? 0}
        </div>
      </div>
    </div>
  )
}
