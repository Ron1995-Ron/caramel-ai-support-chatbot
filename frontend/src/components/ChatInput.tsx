import { useState } from 'react'
import type { FormEvent, KeyboardEvent } from 'react'
import { MAX_MESSAGE_LENGTH } from '../state/types'
import { SendIcon } from './icons'

interface Props {
  disabled: boolean
  /** The text in the box. The page owns it so quick starts can fill it (FR-003). */
  draft: string
  onDraftChange: (text: string) => void
  onSend: (text: string) => void
}

export function ChatInput({ disabled, draft, onDraftChange, onSend }: Props) {
  const [problem, setProblem] = useState<string | null>(null)

  const submit = () => {
    if (disabled) return
    if (draft.trim() === '') {
      setProblem('Type a question before sending.')
      return
    }
    if (draft.length > MAX_MESSAGE_LENGTH) {
      setProblem(`Keep your message under ${MAX_MESSAGE_LENGTH} characters.`)
      return
    }
    setProblem(null)
    onSend(draft)
    onDraftChange('')
  }

  const onFormSubmit = (e: FormEvent) => {
    e.preventDefault()
    submit()
  }

  const onKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      submit()
    }
  }

  return (
    <form className="chat-input glass" onSubmit={onFormSubmit}>
      <div className="chat-input__meta">
        <span className={problem ? 'chat-input__problem' : 'chat-input__count'} role="status">
          {problem ?? `${draft.length} / ${MAX_MESSAGE_LENGTH}`}
        </span>
      </div>
      <div className="chat-input__row">
        <div className="chat-input__field">
          <textarea
            aria-label="Your message"
            placeholder="Ask a question about Caramel AI"
            value={draft}
            rows={1}
            disabled={disabled}
            onChange={(e) => {
              onDraftChange(e.target.value)
              if (problem) setProblem(null)
            }}
            onKeyDown={onKeyDown}
          />
        </div>
        <button type="submit" className="send-button" disabled={disabled} aria-label="Send">
          <SendIcon size={20} />
        </button>
      </div>
    </form>
  )
}
