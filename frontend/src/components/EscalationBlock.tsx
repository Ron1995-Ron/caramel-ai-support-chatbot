import { ESCALATION_SENTENCE } from '../state/constants'

interface Props {
  busy: boolean
  /** Runs the same escalation as the header action. */
  onConnect: () => void
}

/**
 * The assistant's offer to connect a person, shown as its own block when it ends an answer with
 * the escalation sentence (FR-004). Distinct by border, label-like sentence, and button, not only
 * by color (prd-ui-2.md section 10).
 */
export function EscalationBlock({ busy, onConnect }: Props) {
  return (
    <div className="escalation" role="group" aria-label="Connect with a person">
      <p className="escalation__text">{ESCALATION_SENTENCE}</p>
      <button type="button" className="escalation__action" disabled={busy} onClick={onConnect}>
        Connect me
      </button>
    </div>
  )
}
