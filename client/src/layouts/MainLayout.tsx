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
    <SidebarProvider>
      <AppSidebar />
      <SidebarInset>
        <header className="h-16 flex justify-between px-4 py-1">
          <div>
            <SidebarTrigger />
          </div>
          <ModeToggle />
        </header>
        <main className="h-[calc(100vh-72px)]">{children}</main>
      </SidebarInset>
    </SidebarProvider>
  )
}
