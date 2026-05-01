import { API_BASE } from './apiBase.js'

/**
 * Upload a PDF file for analysis.
 * POST /api/v1/papers
 * @param {File} file
 * @returns {Promise<PaperAnalysisResponse>}
 */
export async function uploadPaper(file) {
  const formData = new FormData()
  formData.append('file', file)

  const res = await fetch(`${API_BASE}/api/v1/papers`, {
    method: 'POST',
    body: formData,
  })

  if (!res.ok) {
    let message = `Upload failed (${res.status})`
    try {
      const err = await res.json()
      message = err.message || err.error || message
    } catch {
      // ignore parse errors
    }
    throw new Error(message)
  }

  return res.json()
}

/**
 * Ask a question about the currently uploaded paper.
 * POST /api/v1/papers/ask
 * @param {string} question
 * @returns {Promise<AnswerResponse>}
 */
export async function askQuestion(question) {
  const res = await fetch(`${API_BASE}/api/v1/papers/ask`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ question }),
  })

  if (!res.ok) {
    let message = `Request failed (${res.status})`
    try {
      const err = await res.json()
      message = err.message || err.error || message
    } catch {
      // ignore parse errors
    }
    throw new Error(message)
  }

  return res.json()
}
