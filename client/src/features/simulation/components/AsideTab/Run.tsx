import Typography from '@/components/typography'
import { Button } from '@/components/ui/button'
import { Calendar } from '@/components/ui/calendar'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
} from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { cn } from '@/lib/utils'
import { zodResolver } from '@hookform/resolvers/zod'
import {
  add,
  addDays,
  addMonths,
  addWeeks,
  addYears,
  format,
  isSameMonth,
} from 'date-fns'
import { es } from 'date-fns/locale'
import {
  AlertTriangle,
  CalendarIcon,
  FastForward,
  Loader2,
  PauseIcon,
  Play,
  Rewind,
  Square,
} from 'lucide-react'
import React, { useCallback, useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import {
  useCommandSimulation,
  useStartSimulation,
  useStatusSimulation,
  useStopSimulation,
} from '../../hooks/useSimulation'
import { useSimulationStore } from '../../store/simulation'

const formSchema = z.object({
  simulationType: z.enum(['simple', 'collapse']),
  mode: z.enum(['absolute', 'relative']),
  absolute: z.object({
    from: z.date(),
    to: z.date(),
  }),
  relative: z.object({
    startDate: z.date(),
    duration: z.number().min(1, 'La duración debe ser mayor a 0'),
    unit: z.enum(['days', 'weeks', 'months', 'years']),
  }),
  executionMode: z.enum(['simulation', 'real']),
})
type FormSchema = z.infer<typeof formSchema>

export default function Run() {
  const { mutate: startSimulation, isPending } = useStartSimulation()
  const { mutate: sendCommand } = useCommandSimulation()
  const { mutate: stopSimulation } = useStopSimulation()
  const { data: status } = useStatusSimulation()

  const {
    simulationType: activeSimulationType,
    originalStartDate,
    plgNetwork,
  } = useSimulationStore()

  const [validationError, setValidationError] = React.useState<string | null>(
    null,
  )

  const getInitialValues = useCallback(() => {
    const isRunning = status?.running || false

    if (isRunning && activeSimulationType && originalStartDate) {
      return {
        simulationType: activeSimulationType,
        mode: 'relative' as const,
        executionMode: 'simulation' as const,
        absolute: {
          from: new Date('2025-01-01'),
          to: new Date('2025-01-08'),
        },
        relative: {
          startDate: new Date(originalStartDate),
          duration: 1,
          unit: 'weeks' as const,
        },
      }
    }

    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem('simulationFormState')
      if (saved) {
        try {
          const parsed = JSON.parse(saved)
          if (parsed.absolute?.from)
            parsed.absolute.from = new Date(parsed.absolute.from)
          if (parsed.absolute?.to)
            parsed.absolute.to = new Date(parsed.absolute.to)
          if (parsed.relative?.startDate)
            parsed.relative.startDate = new Date(parsed.relative.startDate)
          return parsed
        } catch {}
      }
    }

    return {
      simulationType: 'simple' as const,
      mode: 'relative' as const,
      executionMode: 'simulation' as const,
      absolute: {
        from: new Date('2025-01-01'),
        to: new Date('2025-01-08'),
      },
      relative: {
        startDate: new Date('2025-01-01'),
        duration: 1,
        unit: 'weeks' as const,
      },
    }
  }, [status?.running, activeSimulationType, originalStartDate])

  const validateOrdersForMonth = useCallback(
    (startDate: Date): boolean => {
      if (!plgNetwork) {
        setValidationError(null)
        return true
      }

      if (!plgNetwork.orders || plgNetwork.orders.length === 0) {
        setValidationError('No hay pedidos disponibles en el sistema')
        return false
      }

      const hasOrdersForMonth = plgNetwork.orders.some((order) => {
        const orderDate = new Date(order.date)
        return isSameMonth(orderDate, startDate)
      })

      if (!hasOrdersForMonth) {
        const monthName = format(startDate, 'MMMM yyyy', { locale: es })
        const errorMessage = `No hay pedidos disponibles para ${monthName}. Por favor, seleccione una fecha diferente.`
        setValidationError(errorMessage)
        return false
      }

      setValidationError(null)
      return true
    },
    [plgNetwork],
  )

  const form = useForm<FormSchema>({
    resolver: zodResolver(formSchema),
    defaultValues: getInitialValues(),
  })

  useEffect(() => {
    const subscription = form.watch((values) => {
      if (typeof window !== 'undefined') {
        localStorage.setItem('simulationFormState', JSON.stringify(values))
      }
    })
    return () => subscription.unsubscribe()
  }, [form])

  const isRunning = status?.running || false
  const currentSimulationType = form.watch('simulationType')
  const currentStartDate = form.watch('relative.startDate')
  const currentMode = form.watch('mode')
  const currentAbsolute = form.watch('absolute')

  useEffect(() => {
    if (!isRunning) {
      let dateToValidate: Date | null = null

      if (currentMode === 'absolute' && currentAbsolute?.from) {
        dateToValidate = currentAbsolute.from
      } else if (currentMode === 'relative' && currentStartDate) {
        dateToValidate = currentStartDate
      }

      if (dateToValidate) {
        const timeoutId = setTimeout(() => {
          validateOrdersForMonth(dateToValidate)
        }, 300)

        return () => clearTimeout(timeoutId)
      }
    }
  }, [
    currentStartDate,
    currentMode,
    currentAbsolute,
    isRunning,
    validateOrdersForMonth,
  ])

  useEffect(() => {
    if (isRunning && activeSimulationType && originalStartDate) {
      if (currentSimulationType !== activeSimulationType) {
        form.reset(getInitialValues())
      }
    }
  }, [
    isRunning,
    activeSimulationType,
    originalStartDate,
    currentSimulationType,
    getInitialValues,
    form,
  ])

  const onSubmit = form.handleSubmit((data) => {
    let startDate: Date
    let endDate: Date = new Date()

    if (data.executionMode === 'real') {
      startDate = new Date()
      endDate = addDays(startDate, 3)
    } else if (data.simulationType === 'collapse') {
      // For "Hasta el colapso": use selected start date
      startDate = data.relative.startDate
      const daysUntilSunday = (7 - startDate.getDay()) % 7 || 7
      endDate = addDays(startDate, 2)
    } else if (data.mode === 'absolute') {
      startDate = data.absolute.from
      endDate = data.absolute.to
    } else {
      startDate = data.relative.startDate
      const { duration, unit } = data.relative
      if (unit === 'days') {
        endDate = addDays(startDate, duration)
      } else if (unit === 'weeks') {
        endDate = addWeeks(startDate, duration)
      } else if (unit === 'months') {
        endDate = addMonths(startDate, duration)
      } else if (unit === 'years') {
        endDate = addYears(startDate, duration)
      }
    }

    // Validar que hay pedidos disponibles para la fecha de inicio (solo para modo simulación)
    if (
      data.executionMode === 'simulation' &&
      !validateOrdersForMonth(startDate)
    ) {
      return // No iniciar la simulación si la validación falla
    }

    startSimulation({
      startTimeOrders: getLocalDateISOString(startDate),
      endTimeOrders: getLocalDateISOString(endDate),
      mode: data.executionMode,
      simulationType: data.simulationType,
      originalStartDate: getLocalDateISOString(startDate),
    })
  })

  const executionMode = form.watch('executionMode')
  const simulationType = form.watch('simulationType')
  const isCollapseMode = simulationType === 'collapse'

  const handleStop = () => {
    stopSimulation()
  }
  const handlePause = () => {
    sendCommand({ command: 'PAUSE' })
  }
  const handleResume = () => {
    sendCommand({ command: 'RESUME' })
  }
  const handleDesaccelerate = () => {
    if (status?.timeAcceleration && status.timeAcceleration >= 1) {
      sendCommand({ command: 'DESACCELERATE' })
    }
  }
  const handleAccelerate = () => {
    if (status?.timeAcceleration && status.timeAcceleration <= 1024) {
      sendCommand({ command: 'ACCELERATE' })
    }
  }
  return (
    <article>
      <Typography variant="h3">Simulación</Typography>

      {/* Mostrar alerta de error de validación */}
      {validationError && (
        <div className="mb-4 p-3 border rounded-md flex items-start gap-2">
          <AlertTriangle className="h-4 w-4 text-600 mt-0.5 flex-shrink-0" />
          <p className="text-sm text-700">{validationError}</p>
        </div>
      )}

      <Form {...form}>
        <form onSubmit={onSubmit}>
          <FormField
            control={form.control}
            name="simulationType"
            render={({ field }) => (
              <FormItem className="mb-4">
                <FormLabel>Tipo de simulación</FormLabel>
                <FormControl>
                  <RadioGroup
                    onValueChange={field.onChange}
                    defaultValue={field.value}
                    disabled={isRunning}
                    className="flex flex-col space-y-2"
                  >
                    <div className="flex items-center space-x-2">
                      <RadioGroupItem value="simple" id="simple" />
                      <Label htmlFor="simple">Simple</Label>
                    </div>
                    <div className="flex items-center space-x-2">
                      <RadioGroupItem value="collapse" id="collapse" />
                      <Label htmlFor="collapse">Hasta el colapso</Label>
                    </div>
                  </RadioGroup>
                </FormControl>
              </FormItem>
            )}
          />

          {isCollapseMode ? (
            <div className="space-y-4">
              <FormField
                control={form.control}
                name="relative.startDate"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Fecha de inicio</FormLabel>
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
                            {field.value ? (
                              format(field.value, 'PPP')
                            ) : (
                              <span>Seleccione una fecha</span>
                            )}
                            <CalendarIcon className="ml-auto h-4 w-4 opacity-50" />
                          </Button>
                        </FormControl>
                      </PopoverTrigger>
                      <PopoverContent className="w-auto p-0" align="start">
                        <Calendar
                          timeZone="UTC"
                          mode="single"
                          autoFocus
                          selected={field.value}
                          onSelect={field.onChange}
                          disabled={isRunning}
                        />
                      </PopoverContent>
                    </Popover>
                  </FormItem>
                )}
              />
              <div className="flex gap-2">
                <FormField
                  control={form.control}
                  name="relative.duration"
                  render={() => (
                    <FormItem className="flex-1">
                      <FormLabel>Duración</FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          min={1}
                          disabled={true}
                          value="Hasta el colapso"
                          readOnly
                        />
                      </FormControl>
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="relative.unit"
                  render={({ field }) => (
                    <FormItem className="flex-1">
                      <FormLabel>Unidad</FormLabel>
                      <Select
                        onValueChange={field.onChange}
                        defaultValue={field.value}
                        disabled={true}
                      >
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder="-" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value="days">Días</SelectItem>
                          <SelectItem value="weeks">Semanas</SelectItem>
                          <SelectItem value="months">Meses</SelectItem>
                          <SelectItem value="years">Años</SelectItem>
                        </SelectContent>
                      </Select>
                    </FormItem>
                  )}
                />
              </div>
            </div>
          ) : (
            <Tabs
              value={form.watch('mode')}
              onValueChange={(value) => {
                form.setValue('mode', value as 'absolute' | 'relative')
              }}
            >
              <TabsList>
                <TabsTrigger
                  value="relative"
                  className="w-full"
                  disabled={isRunning}
                >
                  Relativo
                </TabsTrigger>
                <TabsTrigger
                  value="absolute"
                  className="w-full"
                  disabled={isRunning}
                >
                  Absoluto
                </TabsTrigger>
              </TabsList>
              <TabsContent value="absolute">
                <FormField
                  control={form.control}
                  name="absolute"
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
                            timeZone="UTC"
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
              </TabsContent>
              <TabsContent value="relative" className="space-y-4">
                <FormField
                  control={form.control}
                  name="relative.startDate"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Fecha de inicio</FormLabel>
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
                              {field.value ? (
                                format(field.value, 'PPP')
                              ) : (
                                <span>Seleccione una fecha</span>
                              )}
                              <CalendarIcon className="ml-auto h-4 w-4 opacity-50" />
                            </Button>
                          </FormControl>
                        </PopoverTrigger>
                        <PopoverContent className="w-auto p-0" align="start">
                          <Calendar
                            timeZone="UTC"
                            mode="single"
                            autoFocus
                            selected={field.value}
                            onSelect={field.onChange}
                            disabled={isRunning}
                          />
                        </PopoverContent>
                      </Popover>
                    </FormItem>
                  )}
                />
                <div className="flex gap-2">
                  <FormField
                    control={form.control}
                    name="relative.duration"
                    render={({ field }) => (
                      <FormItem className="flex-1">
                        <FormLabel>Duración</FormLabel>
                        <FormControl>
                          <Input
                            type="number"
                            min={1}
                            disabled={isRunning}
                            {...field}
                            onChange={(e) =>
                              field.onChange(
                                Number.parseInt(e.target.value) || 1,
                              )
                            }
                          />
                        </FormControl>
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="relative.unit"
                    render={({ field }) => (
                      <FormItem className="flex-1">
                        <FormLabel>Unidad</FormLabel>
                        <Select
                          onValueChange={field.onChange}
                          defaultValue={field.value}
                          disabled={isRunning}
                        >
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue placeholder="Seleccione unidad" />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            <SelectItem value="days">Días</SelectItem>
                            <SelectItem value="weeks">Semanas</SelectItem>
                            <SelectItem value="months">Meses</SelectItem>
                            <SelectItem value="years">Años</SelectItem>
                          </SelectContent>
                        </Select>
                      </FormItem>
                    )}
                  />
                </div>
              </TabsContent>
            </Tabs>
          )}

          <div className="mt-4">
            {isRunning ? (
              <div className="flex items-center justify-center gap-2">
                {executionMode === 'simulation' && (
                  <>
                    <Button
                      type="button"
                      size="icon"
                      variant="secondary"
                      onClick={handleDesaccelerate}
                      disabled={(status?.timeAcceleration ?? 1) <= 1}
                    >
                      <Rewind className="h-4 w-4" />
                    </Button>
                    <Button
                      type="button"
                      size="icon"
                      variant="secondary"
                      onClick={status?.paused ? handleResume : handlePause}
                    >
                      {status?.paused ? (
                        <Play className="h-4 w-4" />
                      ) : (
                        <PauseIcon className="h-4 w-4" />
                      )}
                    </Button>
                  </>
                )}
                <Button
                  type="button"
                  size="icon"
                  variant="destructive"
                  onClick={handleStop}
                >
                  <Square className="h-4 w-4" />
                </Button>
                {executionMode === 'simulation' && (
                  <Button
                    type="button"
                    size="icon"
                    variant="secondary"
                    onClick={handleAccelerate}
                    disabled={(status?.timeAcceleration ?? 1) >= 1024}
                  >
                    <FastForward className="h-4 w-4" />
                  </Button>
                )}
              </div>
            ) : (
              <div className="flex flex-col gap-2">
                <Button
                  type="submit"
                  className="w-full"
                  disabled={isPending}
                  onClick={() => form.setValue('executionMode', 'simulation')}
                >
                  {isPending ? (
                    <Loader2 className="animate-spin" />
                  ) : (
                    'Iniciar simulación'
                  )}
                </Button>
              </div>
            )}
          </div>
        </form>
      </Form>
    </article>
  )
}

function getLocalDateISOString(date: Date) {
  return date.toISOString().slice(0, 19) // Remove the 'Z'
}
