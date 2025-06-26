import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
} from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { useForm } from 'react-hook-form'

type TruckForm = {
  code: string
  type: string
  tareWeight: number
  maxCapacity: number
  currentCapacity: number
  fuelCapacity: number
  currentFuel: number
  status: string
}

export default function AddTruckDialog() {
  const form = useForm<TruckForm>({
    defaultValues: {
      code: '',
      type: 'TA',
      tareWeight: 0,
      maxCapacity: 0,
      currentCapacity: 0,
      fuelCapacity: 0,
      currentFuel: 0,
      status: 'ACTIVE',
    },
  })

  function onSubmit(values: TruckForm) {}

  return (
    <Dialog>
      <DialogTrigger asChild>
        <Button>Agregar Camión</Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Agregar Camión</DialogTitle>
          <DialogDescription>Complete los datos del camión.</DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="code"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Código</FormLabel>
                  <FormControl>
                    <Input {...field} required />
                  </FormControl>
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="type"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Tipo</FormLabel>
                  <FormControl>
                    <Select
                      value={field.value}
                      onValueChange={field.onChange}
                      required
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Selecciona un tipo" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="TA">TA</SelectItem>
                        <SelectItem value="TB">TB</SelectItem>
                        <SelectItem value="TC">TC</SelectItem>
                        <SelectItem value="TD">TD</SelectItem>
                      </SelectContent>
                    </Select>
                  </FormControl>
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="tareWeight"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Tara (t)</FormLabel>
                  <FormControl>
                    <Input type="number" {...field} required />
                  </FormControl>
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="maxCapacity"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Capacidad (m³)</FormLabel>
                  <FormControl>
                    <Input type="number" {...field} required />
                  </FormControl>
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="currentCapacity"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Capacidad Actual (m³)</FormLabel>
                  <FormControl>
                    <Input type="number" {...field} required />
                  </FormControl>
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="fuelCapacity"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Combustible (gal)</FormLabel>
                  <FormControl>
                    <Input type="number" {...field} required />
                  </FormControl>
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="currentFuel"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Combustible Actual (gal)</FormLabel>
                  <FormControl>
                    <Input type="number" {...field} required />
                  </FormControl>
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="status"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Estado</FormLabel>
                  <FormControl>
                    <Select
                      value={field.value}
                      onValueChange={field.onChange}
                      required
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Selecciona un estado" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="ACTIVE">Activo</SelectItem>
                        <SelectItem value="MAINTENANCE">
                          Mantenimiento
                        </SelectItem>
                        <SelectItem value="BROKEN_DOWN">Averiado</SelectItem>
                      </SelectContent>
                    </Select>
                  </FormControl>
                </FormItem>
              )}
            />
            <DialogFooter>
              <Button type="submit" disabled>
                Guardar
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
