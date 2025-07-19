import EntityManagement from '@/components/EntityManagement'
import PageLayout from '@/layouts/PageLayout'
import { useMemo } from 'react'
import { stationColumns } from './columns'
import { AddStationDialog } from './components'
import { useStations } from './hooks/useStations'

function Management() {
  // Aquí iría la lógica de acciones masivas, eliminar, editar, etc.
  return <></>
}

export default function StationsFeature() {
  const { data } = useStations()
  const columns = useMemo(() => stationColumns, [])

  return (
    <PageLayout name="Estaciones de Recarga">
      <EntityManagement
        path="/_auth/stations"
        data={data}
        columns={columns}
        Management={Management}
        Dialog={AddStationDialog}
      />
    </PageLayout>
  )
}
