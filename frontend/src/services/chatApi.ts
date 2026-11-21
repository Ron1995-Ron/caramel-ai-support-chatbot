import type { DoneEvent, ErrorEvent, Problem, Rating, TokenEvent } from '../state/types'

const BASE = '/api'

const GENERIC_ERROR = 'Something went wrong. Please try again, or ask for a human agent.'

/** An error with a message that is safe to show to the customer. */
export class ApiError extends Error {
  readonly code: string
  readonly status: number

  constructor(message: string, code: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
  }
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const problem = (await response.json()) as Problem
    return new ApiError(problem.detail ?? GENERIC_ERROR, problem.code ?? 'ERROR', response.status)
  } catch {
    return new ApiError(GENERIC_ERROR, 'ERROR', response.status)
  }
}

export async function createSession(): Promise<string> {
  let response: Response
  try {
    response = await fetch(`${BASE}/sessions`, { method: 'POST' })
  } catch {
    throw new ApiError('Could not reach the support service. Please try again.', 'NETWORK', 0)
  }
  if (!response.ok) throw await toApiError(response)
  const body = (await response.json()) as { sessionId: string }
  return body.sessionId
}

/** A message as the server records it (contracts/chat-api.yaml `Message`). */
export interface ServerMessage {
  id: string
  role: 'customer' | 'assistant'
  content: string
  status: 'complete' | 'failed'
  createdAt: string
  rating?: Rating
}

/** Every message in the current session, in order (FR-006). */
export async function getMessages(sessionId: string): Promise<ServerMessage[]> {
  let response: Response
  try {
    response = await fetch(`${BASE}/sessions/${sessionId}/messages`)
  } catch {
    throw new ApiError('Could not reach the support service.', 'NETWORK', 0)
  }
  if (!response.ok) throw await toApiError(response)
  return (await response.json()) as ServerMessage[]
}

/** Creates a support ticket for a human to follow up on (FR-005). Returns the ticket reference. */
export async function requestEscalation(sessionId: string): Promise<string> {
  let response: Response
  try {
    response = await fetch(`${BASE}/sessions/${sessionId}/escalations`, { method: 'POST' })
  } catch {
    throw new ApiError('Could not reach the support service. Please try again.', 'NETWORK', 0)
  }
  if (!response.ok) throw await toApiError(response)
  const body = (await response.json()) as { ticketRef: string }
  return body.ticketRef
}

/** Records or changes the thumbs up or down on one assistant answer (FR-007). */
export async function sendFeedback(
  sessionId: string,
  messageId: string,
  rating: Rating,
): Promise<void> {
  let response: Response
  try {
    response = await fetch(`${BASE}/sessions/${sessionId}/messages/${messageId}/feedback`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ rating }),
    })
  } catch {
    throw new ApiError('Could not save your feedback. Please try again.', 'NETWORK', 0)
  }
  if (!response.ok) throw await toApiError(response)
}

export interface StreamHandlers {
  onToken: (delta: string) => void
  onDone: (messageId: string) => void
  onError: (message: string, code: string) => void
}

/**
 * Sends a customer message and reads the streamed reply (FR-002). The message goes in a POST
 * body, so this reads the stream with fetch, not EventSource. Exactly one of `onDone` or
 * `onError` is called. A stream that ends with neither is reported as an error (FR-008).
 */
export async function sendMessage(
  sessionId: string,
  content: string,
  handlers: StreamHandlers,
): Promise<void> {
  let finished = false
  const finishDone = (messageId: string) => {
    if (finished) return
    finished = true
    handlers.onDone(messageId)
  }
  const finishError = (message: string, code: string) => {
    if (finished) return
    finished = true
    handlers.onError(message, code)
  }

  try {
    const response = await fetch(`${BASE}/sessions/${sessionId}/messages`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream, application/json',
      },
      body: JSON.stringify({ content }),
    })
    if (!response.ok) {
      const error = await toApiError(response)
      finishError(error.message, error.code)
      return
    }
    if (!response.body) {
      finishError(GENERIC_ERROR, 'NO_STREAM')
      return
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    for (;;) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      // Events are separated by a blank line. Accept both \n\n and \r\n\r\n.
      const blocks = buffer.split(/\r?\n\r?\n/)
      buffer = blocks.pop() ?? ''
      for (const block of blocks) {
        dispatchBlock(block, handlers.onToken, finishDone, finishError)
      }
    }
    if (buffer.trim()) dispatchBlock(buffer, handlers.onToken, finishDone, finishError)
  } catch {
    finishError('The connection was lost. Please try again, or ask for a human agent.', 'NETWORK')
    return
  }
  // The stream closed without a done or error event.
  finishError(GENERIC_ERROR, 'STREAM_ENDED')
}

function dispatchBlock(
  block: string,
  onToken: (delta: string) => void,
  onDone: (messageId: string) => void,
  onError: (message: string, code: string) => void,
): void {
  let event = 'message'
  const dataLines: string[] = []
  for (const line of block.split(/\r?\n/)) {
    if (line.startsWith('event:')) event = line.slice(6).trim()
    else if (line.startsWith('data:')) dataLines.push(line.slice(5).replace(/^ /, ''))
  }
  if (dataLines.length === 0) return
  let data: unknown
  try {
    data = JSON.parse(dataLines.join('\n'))
  } catch {
    return
  }
  if (event === 'token') onToken((data as TokenEvent).delta)
  else if (event === 'done') onDone((data as DoneEvent).messageId)
  else if (event === 'error') {
    const e = data as ErrorEvent
    onError(e.message, e.code)
  }
}
