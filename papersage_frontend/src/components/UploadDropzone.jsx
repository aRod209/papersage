import { useRef, useState } from 'react'

/**
 * PDF file selector with drag-and-drop support.
 * Calls onFileSelect(file) when a valid PDF is chosen.
 */
const UploadDropzone = ({ onFileSelect, disabled }) => {
  const inputRef = useRef(null)
  const [dragging, setDragging] = useState(false)
  const [selectedFile, setSelectedFile] = useState(null)
  const [fileError, setFileError] = useState(null)

  const handleFile = (file) => {
    setFileError(null)
    if (!file) return
    if (file.type !== 'application/pdf') {
      setFileError('Only PDF files are supported.')
      setSelectedFile(null)
      onFileSelect(null)
      return
    }
    setSelectedFile(file)
    onFileSelect(file)
  }

  const handleInputChange = (e) => {
    handleFile(e.target.files[0] || null)
  }

  const handleDrop = (e) => {
    e.preventDefault()
    setDragging(false)
    if (disabled) return
    handleFile(e.dataTransfer.files[0] || null)
  }

  const handleDragOver = (e) => {
    e.preventDefault()
    if (!disabled) setDragging(true)
  }

  const handleDragLeave = () => setDragging(false)

  const handleClick = () => {
    if (!disabled) inputRef.current?.click()
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' || e.key === ' ') handleClick()
  }

  return (
    <div className="space-y-2">
      <div
        role="button"
        tabIndex={disabled ? -1 : 0}
        aria-label="Click or drag a PDF file here to upload"
        onClick={handleClick}
        onKeyDown={handleKeyDown}
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        className={[
          'relative flex flex-col items-center justify-center gap-3 rounded-2xl border-2 border-dashed px-6 py-12 transition-colors cursor-pointer',
          dragging
            ? 'border-[#B3A369] bg-[#B3A369]/10'
            : 'border-slate-300 bg-slate-50 hover:border-[#B3A369]/60 hover:bg-[#F7F5EE]',
          disabled ? 'opacity-50 cursor-not-allowed' : '',
        ].join(' ')}
      >
        <input
          ref={inputRef}
          type="file"
          accept="application/pdf"
          className="sr-only"
          onChange={handleInputChange}
          disabled={disabled}
          aria-hidden="true"
        />
        <div className="w-12 h-12 rounded-full bg-[#003057]/10 flex items-center justify-center text-2xl" aria-hidden="true">
          📄
        </div>
        {selectedFile ? (
          <div className="text-center">
            <p className="font-semibold text-[#003057]">{selectedFile.name}</p>
            <p className="text-sm text-slate-500 mt-0.5">
              {(selectedFile.size / 1024 / 1024).toFixed(2)} MB
            </p>
          </div>
        ) : (
          <div className="text-center">
            <p className="font-medium text-slate-700">
              Drop your PDF here, or <span className="text-[#003057] underline font-semibold">browse</span>
            </p>
            <p className="text-sm text-slate-400 mt-1">PDF files up to 50 MB</p>
          </div>
        )}
      </div>
      {fileError && (
        <p className="text-sm text-red-600 flex items-center gap-1.5" role="alert">
          <span aria-hidden="true">⚠️</span> {fileError}
        </p>
      )}
    </div>
  )
}

export default UploadDropzone
