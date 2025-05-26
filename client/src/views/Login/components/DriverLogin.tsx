import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'
import {
  InputOTP,
  InputOTPGroup,
  InputOTPSlot,
} from '@/components/ui/input-otp'
import { useSessionStore } from '@/store/session'
import { zodResolver } from '@hookform/resolvers/zod'
import { useNavigate } from '@tanstack/react-router'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

export default function DriverLogin() {
  const form = useForm<FormSchema>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      code: '',
    },
  })
  const { setUsername } = useSessionStore()
  const navigate = useNavigate()
  const onSubmit = form.handleSubmit((data) => {
    setUsername(data.code)
    navigate({ to: '/' })
  })
  return (
    <Card>
      <CardHeader>
        <CardTitle>Iniciar Sesión</CardTitle>
        <CardDescription>
          Monitoree los pedidos, rutas, tiempos de entrega y reporte averias.
        </CardDescription>
      </CardHeader>
      <Form {...form}>
        <form onSubmit={onSubmit}>
          <CardContent className="space-y-2">
            <FormField
              control={form.control}
              name="code"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Código de Camión</FormLabel>
                  <FormControl>
                    <InputOTP maxLength={4} {...field}>
                      <InputOTPGroup>
                        <InputOTPSlot index={0} />
                        <InputOTPSlot index={1} />
                        <InputOTPSlot index={2} />
                        <InputOTPSlot index={3} />
                      </InputOTPGroup>
                    </InputOTP>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </CardContent>
          <CardFooter className="flex justify-center mt-2">
            <Button>Ingresar</Button>
          </CardFooter>
        </form>
      </Form>
    </Card>
  )
}

const formSchema = z.object({
  code: z
    .string({
      required_error: 'El código es obligatorio',
    })
    .length(4, 'El código debe tener 4 dígitos'),
})
type FormSchema = z.infer<typeof formSchema>
