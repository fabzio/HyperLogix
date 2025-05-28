import { Button } from '@/components/ui/button'
import { Calendar } from '@/components/ui/calendar'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
} from '@/components/ui/form'
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover'
import { cn } from '@/lib/utils'
import { zodResolver } from '@hookform/resolvers/zod'
import { format } from 'date-fns'
import { CalendarIcon, PauseIcon, Square } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import {
  useStartSimulation,
  useStatusSimulation,
  useStopSimulation,
} from '../../hooks/useSimulation'

export default function Run() {
  const { mutate: startSimulation } = useStartSimulation()
  const { mutate: stopSimulation } = useStopSimulation()
  const { data } = useStatusSimulation()
  const form = useForm<FormSchema>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      date: {
        from: new Date('2025-01-01'),
        to: new Date('2025-01-08'),
      },
    },
  })

  const onSubmit = form.handleSubmit((data) => {
    startSimulation({
      startTimeOrders: data.date.from.toISOString(),
      endTimeOrders: data.date.to.toISOString(),
    })
  })

  const isRunning = data?.running || false

  return (
    <div>
      <Form {...form}>
        <form onSubmit={onSubmit}>
          <FormField
            control={form.control}
            name="date"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Rango de simulación</FormLabel>
                <Popover>
                  <PopoverTrigger asChild>
                    <FormControl>
                      <Button
                        variant="outline"
                        disabled={isRunning}
                        className={cn(
                          'w-[300px] justify-start text-left font-normal',
                          !field.value && 'text-muted-foreground',
                        )}
                      >
                        <CalendarIcon />
                        {field.value?.from ? (
                          field.value.to ? (
                            <>
                              {format(field.value.from, 'LLL dd, y')} -{' '}
                              {format(field.value.to, 'LLL dd, y')}
                            </>
                          ) : (
                            format(field.value.from, 'LLL dd, y')
                          )
                        ) : (
                          <span>Elija el rango</span>
                        )}
                      </Button>
                    </FormControl>
                  </PopoverTrigger>
                  <PopoverContent className="w-auto p-0" align="start">
                    <Calendar
                      initialFocus
                      mode="range"
                      defaultMonth={field.value?.from}
                      selected={field.value}
                      onSelect={field.onChange}
                      numberOfMonths={2}
                      disabled={isRunning}
                    />
                  </PopoverContent>
                </Popover>
              </FormItem>
            )}
          />
          <div className="mt-4">
            {isRunning ? (
              <div className="flex items-center justify-center gap-2">
                <Button size="icon" variant="secondary">
                  <PauseIcon className="h-4 w-4" />
                </Button>
                <Button
                  type="button"
                  size="icon"
                  variant="destructive"
                  onClick={() => stopSimulation()}
                >
                  <Square className="h-4 w-4" />
                </Button>
              </div>
            ) : (
              <Button type="submit" className="w-full">
                Ejecutar
              </Button>
            )}
          </div>
        </form>
      </Form>
    </div>
  )
}

const formSchema = z.object({
  date: z.object({
    from: z.date(),
    to: z.date(),
  }),
})
type FormSchema = z.infer<typeof formSchema>
