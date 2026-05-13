import React, { useState, useRef } from 'react';
import Button from '../atoms/Button';
import Badge from '../atoms/Badge';
import { imageApi } from '../../api/imageApi';

const UploadImageModal = ({ albumId, onClose, onSuccess }) => {
  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);
  const fileInputRef = useRef(null);

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0]);
      setError('');
      setResult(null);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!file) {
      setError('Selecciona una imagen.');
      return;
    }

    setLoading(true);
    setError('');
    
    try {
      const res = await imageApi.uploadImage(albumId, file);
      setResult(res.data); // This has imageStatus, etc.
    } catch (err) {
      setError(err.response?.data?.message || 'Error al subir la imagen.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="glass-card w-full max-w-md p-6 fade-in shadow-2xl">
        <div className="flex justify-between items-center mb-5">
          <h2 className="text-xl font-bold text-white">Subir Imagen</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-white transition-colors" disabled={loading}>
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        
        {!result ? (
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            {error && <div className="text-red-400 text-sm bg-red-500/10 p-2 rounded border border-red-500/20">{error}</div>}
            
            <div 
              className={`border-2 border-dashed rounded-lg p-8 text-center cursor-pointer transition-colors ${file ? 'border-purple-500 bg-purple-500/5' : 'border-gray-600 hover:border-purple-400 hover:bg-[#1a1c26]'}`}
              onClick={() => fileInputRef.current?.click()}
            >
              <input 
                type="file" 
                ref={fileInputRef} 
                className="hidden" 
                accept="image/jpeg,image/png,image/gif"
                onChange={handleFileChange}
              />
              {file ? (
                <div>
                  <p className="text-purple-400 font-medium truncate">{file.name}</p>
                  <p className="text-xs text-gray-500 mt-1">{(file.size / 1024 / 1024).toFixed(2)} MB</p>
                </div>
              ) : (
                <div>
                  <svg className="mx-auto h-10 w-10 text-gray-400 mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                  </svg>
                  <p className="text-gray-300 font-medium">Haz clic para seleccionar</p>
                  <p className="text-xs text-gray-500 mt-1">PNG, JPG, GIF hasta 10MB</p>
                </div>
              )}
            </div>
            
            <div className="flex gap-3 justify-end mt-2">
              <Button type="button" variant="ghost" onClick={onClose} disabled={loading}>Cancelar</Button>
              <Button type="submit" loading={loading} disabled={!file}>Subir y Analizar</Button>
            </div>
          </form>
        ) : (
          <div className="flex flex-col items-center text-center gap-4 py-4 fade-in">
            <div className="mb-2">
              <Badge status={result.imageStatus} className="text-lg px-4 py-1" />
            </div>
            <h3 className="text-lg font-medium text-white">Análisis Completado</h3>
            
            <div className="w-full bg-[#12141a] p-4 rounded border border-gray-800 text-left text-sm mt-2">
              <p className="text-gray-400 mb-1">Archivo: <span className="text-gray-200">{result.originalName}</span></p>
              <p className="text-gray-400 mb-1">Entropía LSB: <span className="text-gray-200 font-mono">{result.lsbEntropyScore?.toFixed(4) || 'N/A'}</span></p>
              {result.analysisResult && (
                <p className="text-gray-400 mt-2">Detalles: <span className="text-gray-200">{result.analysisResult}</span></p>
              )}
            </div>

            <p className="text-sm text-gray-400 mt-2">
              {result.imageStatus === 'CLEAN' || result.imageStatus === 'APPROVED'
                ? 'La imagen es segura y ya está disponible en el álbum.'
                : 'La imagen ha sido enviada a cuarentena para revisión manual por un supervisor.'}
            </p>

            <Button className="w-full mt-4" onClick={() => {
              onSuccess();
              onClose();
            }}>
              Entendido
            </Button>
          </div>
        )}
      </div>
    </div>
  );
};

export default UploadImageModal;
