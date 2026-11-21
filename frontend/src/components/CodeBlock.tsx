interface Props {
  code: string
}

/** Inset block for a code sample. Long lines scroll inside the block, not the page (UI-11). */
export function CodeBlock({ code }: Props) {
  return (
    <pre className="code-block" tabIndex={0}>
      <code>{code}</code>
    </pre>
  )
}
