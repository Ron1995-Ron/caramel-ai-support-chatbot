package com.caramelai.support.chat.knowledge;

/**
 * Where the assistant gets Caramel AI support content. Callers depend on this interface only, so a
 * retrieval-based implementation can replace the static one without changing them (research.md R2,
 * constitution: grounding sits behind a replaceable boundary).
 */
public interface KnowledgeSource {

  /**
   * Returns the support content to give the model for this question. A static implementation may
   * ignore the question and return everything. A retrieval implementation would return only the
   * relevant passages.
   */
  String contextFor(String question);
}
