interface Props {
  disabled: boolean
  onEscalate: () => void
}

/**
 * Always visible, one click: asks for a human agent (FR-005, SC-005). The result appears in the
 * chat as a normal reply, with the ticket reference or a clear error.
 */
export function EscalationButton({ disabled, onEscalate }: Props) {
  return (
    <button
      type="button"
      className="escalation-button"
      disabled={disabled}
      onClick={onEscalate}
      title="Creates a support ticket so a person can follow up"
    >
      Talk to a person
    </button>
  )
}
