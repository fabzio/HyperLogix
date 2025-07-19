import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form' // schemas/report-incident-schema.ts
import { z } from 'zod'
import { useReportIncident } from '../hooks/useOperationMutations'

interface Props {
  open: boolean
  onClose: () => void
  truckCode: string
}

export default function BreakdownForm({ onClose, open, truckCode }: Props) {
  const form = useForm<ReportIncidentSchema>({
    resolver: zodResolver(reportIncidentSchema),
    defaultValues: {
      truckCode: '',
      incidentType: 'TI1',
      incidentTrun: 'T1',
    },
  })
  form.setValue('truckCode', truckCode)

  const reportIncidentMutation = useReportIncident()

  const onSubmit = (values: ReportIncidentSchema) => {
    console.log('Reporting incident:', values)
    reportIncidentMutation.mutate(
      { ...values, truckCode },
      {
        onSuccess: () => {
          form.reset()
          onClose()
        },
      },
    )
  }

  return (
    <Dialog onOpenChange={onClose} open={open}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Reportar avería</DialogTitle>
          <DialogDescription>
            Reporta una avería del camión {truckCode} para que el equipo de
            operaciones pueda tomar las acciones necesarias.
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="incidentType"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Tipo de Incidente</FormLabel>
                  <FormControl>
                    <Select
                      onValueChange={field.onChange}
                      defaultValue={field.value}
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Selecciona un tipo" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="TI1">Llanta desinflada (TI1)</SelectItem>
                        <SelectItem value="TI2">Fallo de motor (TI2)</SelectItem>
                        <SelectItem value="TI3">Choque (TI3)</SelectItem>
                      </SelectContent>
                    </Select>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="incidentTrun"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Turno</FormLabel>
                  <FormControl>
                    <Select
                      onValueChange={field.onChange}
                      defaultValue={field.value}
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Selecciona un turno" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="T1">T1 - Mañana</SelectItem>
                        <SelectItem value="T2">T2 - Tarde</SelectItem>
                        <SelectItem value="T3">T3 - Noche</SelectItem>
                      </SelectContent>
                    </Select>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={onClose}>
                Cancelar
              </Button>
              <Button type="submit" disabled={reportIncidentMutation.isPending}>
                {reportIncidentMutation.isPending
                  ? 'Reportando...'
                  : 'Reportar'}
              </Button>
            </div>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}

export const reportIncidentSchema = z.object({
  truckCode: z.string().min(1, 'Debe ingresar un código de camión'),
  incidentType: z.enum(['TI1', 'TI2', 'TI3']),
  incidentTrun: z.enum(['T1', 'T2', 'T3']),
})

export type ReportIncidentSchema = z.infer<typeof reportIncidentSchema>
