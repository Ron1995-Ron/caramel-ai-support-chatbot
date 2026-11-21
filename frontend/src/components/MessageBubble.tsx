import { ESCALATION_SENTENCE } from '../state/constants'
import type { ChatMessage, Rating } from '../state/types'
import { AnswerText } from './AnswerText'
import { BrandMark } from './BrandMark'
import { EscalationBlock } from './EscalationBlock'
import { FeedbackButtons } from './FeedbackButtons'

interface Props {
  message: ChatMessage
  /** True while another request is in flight. Actions are disabled so requests do not overlap. */
  busy: boolean
  onRate: (messageId: string, rating: Rating) => Promise<void>
  onEscalate: () => void
  onRetry: (messageId: string) => void
}

/** Ids starting with `local-` are not on the server yet, so they cannot be rated. */
const isOnServer = (id: string) => !id.startsWith('local-')

/** Separates the answer from the escalation offer when an answer ends with the offer. */
function splitOffer(content: string): { body: string; offer: boolean } {
  const trimmed = content.trimEnd()
  if (!trimmed.endsWith(ESCALATION_SENTENCE)) return { body: content, offer: false }
  return { body: trimmed.slice(0, -ESCALATION_SENTENCE.length).trimEnd(), offer: true }
}

export function MessageBubble({ message, busy, onRate, onEscalate, onRetry }: Props) {
  if (message.role === 'customer') {
    return (
      <div className="customer-message">
        <span className="sr-only">You: </span>
        <p className="customer-message__text">{message.content}</p>
      </div>
    )
  }

  if (message.status === 'failed') {
    const ticket = message.failure === 'ticket'
    return (
      <div className="answer">
        <BrandMark />
        <div className="answer__card answer__card--error glass" role="alert">
          <p className="answer__text">
            {ticket
              ? 'Could not create a ticket. Try again.'
              : 'The assistant could not answer. Your message was not lost.'}
          </p>
          <button
            type="button"
            className="button-secondary"
            disabled={busy}
            onClick={() => onRetry(message.id)}
          >
            Try again
          </button>
        </div>
      </div>
    )
  }

  const waiting = message.status === 'streaming' && message.content === ''
  const canRate = message.status === 'complete' && isOnServer(message.id)
  const { body, offer } = splitOffer(message.content)

  return (
    <div className="answer">
      <BrandMark />
      <div className="answer__card glass" aria-busy={message.status === 'streaming'}>
        <span className="sr-only">Caramel AI Support: </span>
        {waiting ? <p className="answer__waiting">Thinking</p> : body && <AnswerText text={body} />}
        {offer && message.status === 'complete' && (
          <EscalationBlock busy={busy} onConnect={onEscalate} />
        )}
        {canRate && (
          <FeedbackButtons rating={message.rating} onRate={(r) => onRate(message.id, r)} />
        )}
      </div>
    </div>
  )
}
