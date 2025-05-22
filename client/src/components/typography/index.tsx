import { cn } from '@/lib/utils'

type TypographyVariant =
  | 'h1'
  | 'h2'
  | 'h3'
  | 'h4'
  | 'p'
  | 'blockquote'
  | 'inlineCode'
  | 'small'
  | 'muted'
  | 'ul'

const variantMap: Record<TypographyVariant, React.ElementType> = {
  h1: 'h1',
  h2: 'h2',
  h3: 'h3',
  h4: 'h4',
  p: 'p',
  blockquote: 'blockquote',
  inlineCode: 'code',
  small: 'small',
  muted: 'p',
  ul: 'ul',
}

const variantClasses: Record<TypographyVariant, string> = {
  h1: 'scroll-m-20 text-4xl font-extrabold tracking-tight lg:text-5xl',
  h2: 'scroll-m-20 text-3xl font-semibold tracking-tight first:mt-0',
  h3: 'scroll-m-20 text-2xl font-semibold tracking-tight',
  h4: 'scroll-m-20 text-xl font-semibold tracking-tight',
  p: 'leading-7 [&:not(:first-child)]:mt-6',
  blockquote: 'mt-6 border-l-2 pl-6 italic',
  inlineCode:
    'relative rounded bg-muted px-[0.3rem] py-[0.2rem] font-mono text-sm font-semibold',
  small: 'text-sm font-medium leading-none',
  muted: 'text-sm text-muted-foreground',
  ul: 'my-6 ml-6 list-disc [&>li]:mt-2',
}

interface TypographyProps extends React.HTMLAttributes<HTMLElement> {
  variant: TypographyVariant
  className?: string
  children: React.ReactNode
}

export default function Typography({
  variant,
  className,
  children,
  ...props
}: TypographyProps) {
  const Component = variantMap[variant] || 'p'
  const mergedClassName = cn(variantClasses[variant], className)
  return (
    <Component className={mergedClassName} {...props}>
      {children}
    </Component>
  )
}
