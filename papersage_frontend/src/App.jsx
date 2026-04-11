import { useState } from 'react'
import UploadPage from './pages/UploadPage.jsx'
import ResultsPage from './pages/ResultsPage.jsx'

/**
 * Root component — manages view state between Upload and Results.
 * No router needed: only two views exist.
 */
const App = () => {
  const [view, setView] = useState('upload') // 'upload' | 'results'
  const [analysis, setAnalysis] = useState(null)

  const handleUploadSuccess = (data) => {
    setAnalysis(data)
    setView('results')
  }

  const handleReset = () => {
    setAnalysis(null)
    setView('upload')
  }

  if (view === 'results' && analysis) {
    return <ResultsPage analysis={analysis} onReset={handleReset} />
  }

  return <UploadPage onSuccess={handleUploadSuccess} />
}

export default App
