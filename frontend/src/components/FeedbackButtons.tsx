import { useState } from 'react'
import type { Rating } from '../state/types'
import { ThumbDownIcon, ThumbUpIcon } from './icons'

interface Props {
  rating?: Rating
  onRate: (rating: Rating) => Promise<void>
}

/** Thumbs up and down on one answer. Choosing the other thumb changes the rating (FR-007). */
export function FeedbackButtons({ rating, onRate }: Props) {
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const choose = async (next: Rating) => {
    if (busy || next === rating) return
    setBusy(true)
    setError(null)
    try {
      await onRate(next)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not save your feedback.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="feedback">
      <button
        type="button"
        className={`feedback__button${rating === 'up' ? ' feedback__button--active' : ''}`}
        aria-label="Helpful answer"
        aria-pressed={rating === 'up'}
        disabled={busy}
        onClick={() => void choose('up')}
      >
        <ThumbUpIcon size={16} />
        <span className="feedback__tip" aria-hidden="true">
          Helpful
        </span>
      </button>
      <button
        type="button"
        className={`feedback__button${rating === 'down' ? ' feedback__button--active' : ''}`}
        aria-label="Not helpful"
        aria-pressed={rating === 'down'}
        disabled={busy}
        onClick={() => void choose('down')}
      >
        <ThumbDownIcon size={16} />
        <span className="feedback__tip" aria-hidden="true">
          Not helpful
        </span>
      </button>
      {rating && !error && <span className="feedback__note">Feedback recorded</span>}
      {error && (
        <span className="feedback__error" role="alert">
          {error}
        </span>
      )}
    </div>
  )
}
