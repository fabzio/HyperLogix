interface Props {
  size?: number
}
export default function Logo({ size }: Props) {
  return (
    <picture>
      <img src="/logo192.png" width={size} alt="logo" />
    </picture>
  )
}
