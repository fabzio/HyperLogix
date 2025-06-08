import { ModeToggle } from '@/components/mode-togle'
import {
  SidebarInset,
  SidebarProvider,
  SidebarTrigger,
} from '@/components/ui/sidebar'
import type { PropsWithChildren } from 'react'
import AppSidebar from './Sidebar'

type Props = PropsWithChildren
export default function MainLayout({ children }: Props) {
  return (
    <div className="min-h-screen relative">
      <SidebarProvider>
        <AppSidebar />
        <SidebarInset>
          <header className="flex justify-between px-4 py-1">
            <div>
              <SidebarTrigger />
            </div>
            <ModeToggle />
          </header>
          <main className="h-[calc(100vh-44px)] bg-transparent">
            {children}
          </main>
        </SidebarInset>
      </SidebarProvider>
    </div>
  )
}
