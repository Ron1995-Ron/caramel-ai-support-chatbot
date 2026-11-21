/** Maximum customer message length (data-model.md: "1 to 4,000 characters, not blank"). */
export const MAX_MESSAGE_LENGTH = 4000

export type Role = 'customer' | 'assistant'

/** `streaming` exists only in the browser, while an answer is still arriving. */
export type MessageStatus = 'streaming' | 'complete' | 'failed'

export type Rating = 'up' | 'down'

export interface ChatMessage {
  id: string
  role: Role
  content: string
  status: MessageStatus
  createdAt: string
  rating?: Rating
  /**
   * Why an assistant message failed. The UI shows fixed copy for each kind and never shows
   * provider error text (UI-8). `answer`: the assistant could not reply. `ticket`: the escalation
   * request failed.
   */
  failure?: 'answer' | 'ticket'
}

/** Server-Sent Event payloads from the contract (contracts/chat-api.yaml). */
export interface TokenEvent {
  delta: string
}

export interface DoneEvent {
  messageId: string
}

export interface ErrorEvent {
  code: string
  message: string
}

/** RFC 9457 problem body returned for non-streaming errors. */
export interface Problem {
  title?: string
  detail?: string
  status?: number
  code?: string
}
