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
  InputOTP,
  InputOTPGroup,
  InputOTPSlot,
} from '@/components/ui/input-otp'
import { Label } from '@/components/ui/label'
import { useNavigate } from '@tanstack/react-router'

export default function DriverLogin() {
  const navigate = useNavigate()
  return (
    <Card>
      <CardHeader>
        <CardTitle>Iniciar Sesión</CardTitle>
        <CardDescription>
          Monitoree los pedidos, rutas, tiempos de entrega y reporte averias.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-2">
        <div className="space-y-1">
          <Label htmlFor="code">Código de Camión</Label>
          <InputOTP maxLength={4} id="code">
            <InputOTPGroup>
              <InputOTPSlot index={0} />
              <InputOTPSlot index={1} />
              <InputOTPSlot index={2} />
              <InputOTPSlot index={3} />
            </InputOTPGroup>
          </InputOTP>
        </div>
      </CardContent>
      <CardFooter className="flex justify-center">
        <Button
          onClick={() => {
            navigate({ to: '/' })
          }}
        >
          Ingresar
        </Button>
      </CardFooter>
    </Card>
  )
}
