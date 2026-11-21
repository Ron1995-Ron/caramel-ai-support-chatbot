interface Props {
  /** Fills the message box with the starter question. Nothing is sent (FR-003). */
  onPick: (question: string) => void
}

// Starter questions are placeholders until the support team confirms the topics
// (specs/002-chat-ui-v2/spec.md, Assumptions). They ask only, and state no product facts.
const STARTERS = [
  { label: 'Setup', question: 'How do I set up Caramel AI for my first request?' },
  { label: 'API usage', question: 'How do I call the Caramel AI API?' },
  {
    label: 'Errors',
    question: 'I am getting an error from the Caramel AI API. What does it mean?',
  },
]

export function QuickStarts({ onPick }: Props) {
  return (
    <div className="quick-starts" role="group" aria-label="Starter questions">
      {STARTERS.map((s) => (
        <button
          key={s.label}
          type="button"
          className="chip glass"
          onClick={() => onPick(s.question)}
        >
          {s.label}
        </button>
      ))}
    </div>
  )
}
