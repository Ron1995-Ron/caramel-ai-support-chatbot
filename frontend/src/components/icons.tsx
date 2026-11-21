import type { ReactNode } from 'react'

interface IconProps {
  size?: number
}

interface IconFrameProps extends IconProps {
  children: ReactNode
  rotate?: boolean
}

/** Line icons. They are decorative: the button that holds them carries the accessible name. */
function Icon({ size = 20, children, rotate = false }: IconFrameProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
      style={rotate ? { transform: 'rotate(180deg)' } : undefined}
    >
      {children}
    </svg>
  )
}

export function ThumbUpIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M7 11v9H4v-9h3z" />
      <path d="M7 11l4-7c1.1 0 2 .9 2 2v3h5.5a2 2 0 0 1 2 2.3l-1.3 6.5A2 2 0 0 1 17.2 20H7" />
    </Icon>
  )
}

export function ThumbDownIcon(props: IconProps) {
  return (
    <Icon {...props} rotate>
      <path d="M7 11v9H4v-9h3z" />
      <path d="M7 11l4-7c1.1 0 2 .9 2 2v3h5.5a2 2 0 0 1 2 2.3l-1.3 6.5A2 2 0 0 1 17.2 20H7" />
    </Icon>
  )
}

export function SendIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M12 19V5" />
      <path d="M5 12l7-7 7 7" />
    </Icon>
  )
}

export function BackIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M15 18l-6-6 6-6" />
    </Icon>
  )
}
