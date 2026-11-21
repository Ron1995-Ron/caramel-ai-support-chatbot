import { useState } from 'react'
import { BackIcon } from '../components/icons'
import { ChatInput } from '../components/ChatInput'
import { EscalationButton } from '../components/EscalationButton'
import { MessageList } from '../components/MessageList'
import { QuickStarts } from '../components/QuickStarts'
import { useChat } from '../state/useChat'

export function ChatPage() {
  const { messages, busy, startError, ready, send, escalate, retry, rate, retryStart } = useChat()
  const [draft, setDraft] = useState('')

  return (
    <main className="chat-page">
      <header className="chat-page__header">
        {/* There is no earlier screen to return to, so the control is shown but not active. */}
        <button type="button" className="icon-button glass" aria-label="Back" disabled>
          <BackIcon size={20} />
        </button>
        <div className="chat-page__title">
          <h1>Caramel AI Support</h1>
          <p>Technical help for LLM inferencing</p>
        </div>
        {ready && <EscalationButton disabled={busy} onEscalate={escalate} />}
      </header>

      {startError ? (
        <div className="chat-page__error glass" role="alert">
          <p>{startError}</p>
          <button type="button" className="button-secondary" onClick={retryStart}>
            Try again
          </button>
        </div>
      ) : (
        <>
          {messages.length === 0 && <QuickStarts onPick={setDraft} />}
          <MessageList
            messages={messages}
            busy={busy}
            onRate={rate}
            onEscalate={escalate}
            onRetry={retry}
          />
          <ChatInput
            disabled={!ready || busy}
            draft={draft}
            onDraftChange={setDraft}
            onSend={send}
          />
        </>
      )}
    </main>
  )
}
