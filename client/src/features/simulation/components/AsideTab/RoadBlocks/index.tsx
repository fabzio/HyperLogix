import Typography from '@/components/typography'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { RoadblockDetail } from '@/features/simulation/components/AsideTab/RoadBlocks/RoadBlockDetail.tsx'
import { useSimulationStore } from '@/features/simulation/store/simulation'
import { useNavigate, useSearch } from '@tanstack/react-router'

export default function RoadBlocks() {
  const { plgNetwork, simulationTime } = useSimulationStore()
  const blocks = plgNetwork?.roadblocks ?? []
  const { roadblockStart } = useSearch({ from: '/_auth/simulacion' })
  const navigate = useNavigate({ from: '/simulacion' })

  // Filtra bloqueos activos en el tiempo actual
  const activeBlocks = blocks.filter(
    (b) =>
      simulationTime && simulationTime >= b.start && simulationTime <= b.end,
  )

  // Ordena por fecha de inicio ascendente
  const sortedBlocks = activeBlocks
    .slice()
    .sort((a, b) => new Date(a.start).getTime() - new Date(b.start).getTime())

  // Función para extraer solo la hora
  const getHour = (isoString: string) =>
    new Date(isoString).toLocaleTimeString([], {
      hour: '2-digit',
      minute: '2-digit',
    })

  if (roadblockStart) {
    return <RoadblockDetail />
  }

  return (
    <article className="h-full">
      <Typography variant="h3" className="mb-4">
        Bloqueos activos
      </Typography>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Inicio</TableHead>
            <TableHead>Fin</TableHead>
            <TableHead>Nodos bloqueados</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {sortedBlocks.length > 0 ? (
            sortedBlocks.map((block) => (
              <TableRow
                key={block.start}
                className="cursor-pointer hover:bg-muted"
                onClick={() =>
                  navigate({ search: { roadblockStart: block.start } })
                }
              >
                <TableCell>{getHour(block.start)}</TableCell>
                <TableCell>{getHour(block.end)}</TableCell>
                <TableCell>{block.blockedNodes.length}</TableCell>
              </TableRow>
            ))
          ) : (
            <TableRow>
              <TableCell colSpan={3} className="text-center">
                No hay bloqueos activos
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
    </article>
  )
}
