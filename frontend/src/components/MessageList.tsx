import { useEffect, useRef, useState } from 'react'
import type { ChatMessage, Rating } from '../state/types'
import { MessageBubble } from './MessageBubble'

interface Props {
  messages: ChatMessage[]
  busy: boolean
  onRate: (messageId: string, rating: Rating) => Promise<void>
  onEscalate: () => void
  onRetry: (messageId: string) => void
}

export function MessageList({ messages, busy, onRate, onEscalate, onRetry }: Props) {
  const endRef = useRef<HTMLDivElement>(null)
  const [announcement, setAnnouncement] = useState('')
  const announcedId = useRef<string | null>(null)

  useEffect(() => {
    endRef.current?.scrollIntoView?.({ block: 'end' })
  }, [messages])

  // Screen readers hear each completed answer once. The visible text streams token by token, so
  // the list itself is not live. Errors use role="alert" on their own cards.
  const latest = [...messages]
    .reverse()
    .find((m) => m.role === 'assistant' && m.status === 'complete' && m.content !== '')
  useEffect(() => {
    if (!latest || latest.id === announcedId.current) return
    announcedId.current = latest.id
    setAnnouncement(latest.content)
  }, [latest])

  return (
    <div className="message-list" role="log" aria-live="off" aria-label="Chat messages">
      {messages.length === 0 && (
        <p className="message-list__empty">Ask about setup, API usage, or an error message.</p>
      )}
      {messages.map((m) => (
        <MessageBubble
          key={m.id}
          message={m}
          busy={busy}
          onRate={onRate}
          onEscalate={onEscalate}
          onRetry={onRetry}
        />
      ))}
      <div ref={endRef} />
      <div className="sr-only" role="status">
        {announcement}
      </div>
    </div>
  )
}
