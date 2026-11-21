import { useCallback, useEffect, useRef, useState } from 'react'
import {
  createSession,
  getMessages,
  requestEscalation,
  sendFeedback,
  sendMessage,
} from '../services/chatApi'
import type { ServerMessage } from '../services/chatApi'
import type { ChatMessage, Rating } from './types'

interface ChatState {
  messages: ChatMessage[]
  /** True while an answer is streaming or a ticket is being created. */
  busy: boolean
  /** Set when a session could not be started. The chat cannot be used until it is retried. */
  startError: string | null
  ready: boolean
  send: (text: string) => void
  escalate: () => void
  /** Retries a failed answer (resends its question) or a failed ticket request. */
  retry: (messageId: string) => void
  rate: (messageId: string, rating: Rating) => Promise<void>
  retryStart: () => void
}

let localCounter = 0
const localId = () => `local-${++localCounter}`

const fromServer = (m: ServerMessage): ChatMessage => ({
  id: m.id,
  role: m.role,
  content: m.content,
  status: m.status,
  createdAt: m.createdAt,
  rating: m.rating,
  failure: m.status === 'failed' ? 'answer' : undefined,
})

/**
 * Chat state for one session. Holds no secrets: the browser never sees the AI provider key.
 * Whatever happens to a request, `busy` returns to false, so the UI is never left stuck (FR-008).
 * A page reload starts a new session. Old conversations are not shown (FR-011).
 */
export function useChat(): ChatState {
  const [sessionId, setSessionId] = useState<string | null>(null)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [busy, setBusy] = useState(false)
  const [startError, setStartError] = useState<string | null>(null)
  const starting = useRef(false)

  const start = useCallback(() => {
    if (starting.current) return
    starting.current = true
    setStartError(null)
    createSession()
      .then((id) => setSessionId(id))
      .catch((e: unknown) =>
        setStartError(e instanceof Error ? e.message : 'Could not start a chat. Please try again.'),
      )
      .finally(() => {
        starting.current = false
      })
  }, [])

  useEffect(() => {
    start()
  }, [start])

  /**
   * After the connection drops mid-answer, ask the server what it recorded so the customer sees
   * the full session, in order (FR-006). If the server cannot be reached either, keep the view.
   */
  const resync = useCallback((id: string) => {
    getMessages(id)
      .then((server) => setMessages(server.map(fromServer)))
      .catch(() => undefined)
  }, [])

  const send = useCallback(
    (text: string) => {
      if (!sessionId || busy) return
      const now = new Date().toISOString()
      const assistantLocalId = localId()
      setBusy(true)
      setMessages((prev) => [
        ...prev,
        { id: localId(), role: 'customer', content: text, status: 'complete', createdAt: now },
        {
          id: assistantLocalId,
          role: 'assistant',
          content: '',
          status: 'streaming',
          createdAt: now,
        },
      ])

      const update = (patch: (m: ChatMessage) => ChatMessage) =>
        setMessages((prev) => prev.map((m) => (m.id === assistantLocalId ? patch(m) : m)))

      void sendMessage(sessionId, text, {
        onToken: (delta) => update((m) => ({ ...m, content: m.content + delta })),
        onDone: (messageId) => {
          update((m) => ({ ...m, id: messageId, status: 'complete' }))
          setBusy(false)
        },
        onError: (_message, code) => {
          // The provider's message is not shown to the customer (UI-8). The fixed copy is in the UI.
          update((m) => ({ ...m, status: 'failed', failure: 'answer' }))
          setBusy(false)
          if (code === 'NETWORK' || code === 'STREAM_ENDED') resync(sessionId)
        },
      }).catch(() => {
        update((m) => ({ ...m, status: 'failed', failure: 'answer' }))
        setBusy(false)
      })
    },
    [sessionId, busy, resync],
  )

  const escalate = useCallback(() => {
    if (!sessionId || busy) return
    setBusy(true)
    const add = (m: Omit<ChatMessage, 'id' | 'role' | 'createdAt'>) =>
      setMessages((prev) => [
        ...prev,
        { id: localId(), role: 'assistant', createdAt: new Date().toISOString(), ...m },
      ])

    requestEscalation(sessionId)
      .then((ticketRef) =>
        add({
          content: `I've created a support ticket for you (reference ${ticketRef}). A member of our support team will follow up. You can keep asking me questions in the meantime.`,
          status: 'complete',
        }),
      )
      .catch(() => add({ content: '', status: 'failed', failure: 'ticket' }))
      .finally(() => setBusy(false))
  }, [sessionId, busy])

  const retry = useCallback(
    (messageId: string) => {
      if (busy) return
      const index = messages.findIndex((m) => m.id === messageId)
      const failed = messages[index]
      if (index < 0 || !failed) return

      if (failed.failure === 'ticket') {
        setMessages((prev) => prev.filter((m) => m.id !== messageId))
        escalate()
        return
      }

      // Resend the customer question that this failed answer responded to. Remove both, so the
      // question is not shown twice.
      const question = messages
        .slice(0, index)
        .reverse()
        .find((m) => m.role === 'customer')
      if (!question) return
      setMessages((prev) => prev.filter((m) => m.id !== messageId && m.id !== question.id))
      send(question.content)
    },
    [messages, busy, send, escalate],
  )

  const rate = useCallback(
    async (messageId: string, rating: Rating) => {
      if (!sessionId) return
      await sendFeedback(sessionId, messageId, rating)
      setMessages((prev) => prev.map((m) => (m.id === messageId ? { ...m, rating } : m)))
    },
    [sessionId],
  )

  return {
    messages,
    busy,
    startError,
    ready: sessionId !== null,
    send,
    escalate,
    retry,
    rate,
    retryStart: start,
  }
}
