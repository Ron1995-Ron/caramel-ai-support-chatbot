import { CodeBlock } from './CodeBlock'

interface Props {
  text: string
}

/**
 * Renders an answer. Text between fenced code blocks becomes paragraphs, and each fenced block
 * becomes a CodeBlock. Output is React text nodes only, never HTML, so provider output cannot
 * inject markup. An unclosed fence is left as plain text, so nothing is hidden.
 */
export function AnswerText({ text }: Props) {
  // Splitting on a capturing group puts the code bodies at odd indexes.
  const parts = text.split(/```[^\n]*\n?([\s\S]*?)```/g)
  return (
    <>
      {parts.map((part, i) => {
        if (i % 2 === 1) return <CodeBlock key={i} code={part.replace(/\n$/, '')} />
        const prose = part.replace(/^\n+|\n+$/g, '')
        return prose ? (
          <p key={i} className="answer__text">
            {prose}
          </p>
        ) : null
      })}
    </>
  )
}
