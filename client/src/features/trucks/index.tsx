import EntityManagement from '@/components/EntityManagement'
import { useFilters } from '@/hooks/useFilters'
import PageLayout from '@/layouts/PageLayout'
import { useMemo } from 'react'
import { truckColumns } from './columns'
import { AddTruckDialog } from './components'
import { useTrucks } from './hooks/useTrucks'
import { FaTruck } from 'react-icons/fa'
import { Truck } from 'lucide-react'

function Management() {
  // Aquí iría la lógica de acciones masivas, eliminar, editar, etc.
  return null
}

export default function TrucksFeature() {
  const { filters } = useFilters('/_auth/trucks')
  const { data } = useTrucks(filters)
  const columns = useMemo(() => truckColumns, [])
  return (
    <PageLayout
      name={
        <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Truck className="w-5 h-5" />
          Camiones
        </span>
      }
    >
      <EntityManagement
        data={data}
        columns={columns}
        Management={Management}
        Dialog={AddTruckDialog}
      />
    </PageLayout>
  )
}
