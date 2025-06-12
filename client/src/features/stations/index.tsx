import EntityManagement from '@/components/EntityManagement'
import PageLayout from '@/layouts/PageLayout'
import { useMemo } from 'react'
import { stationColumns } from './columns'
import { AddStationDialog } from './components'
import { useStations } from './hooks/useStations'
import { MdEvStation } from 'react-icons/md'
import { Fuel } from 'lucide-react'

function Management() {
  // Aquí iría la lógica de acciones masivas, eliminar, editar, etc.
  return null
}

export default function StationsFeature() {
  const { data } = useStations()
  const columns = useMemo(() => stationColumns, [])

  return (
    <PageLayout
      name={
        <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Fuel className="w-5 h-5" />
          Estaciones de Recarga
        </span>
      }
    >
      <EntityManagement
        data={data}
        columns={columns}
        Management={Management}
        Dialog={AddStationDialog}
      />
    </PageLayout>
  )
}
