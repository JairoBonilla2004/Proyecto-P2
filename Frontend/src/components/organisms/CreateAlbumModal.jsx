import React, { useState } from 'react';
import FormField from '../molecules/FormField';
import Button from '../atoms/Button';
import { albumApi } from '../../api/albumApi';

const CreateAlbumModal = ({ onClose, onSuccess }) => {
  const [formData, setFormData] = useState({ title: '', description: '', isPublic: true });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    
    try {
      const payload = { title: formData.title, description: formData.description, public: formData.isPublic };
      await albumApi.createAlbum(payload);
      onSuccess();
    } catch (err) {
      setError(err.response?.data?.message || 'Error al crear álbum.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="glass-card w-full max-w-md p-6 fade-in shadow-2xl">
        <div className="flex justify-between items-center mb-5">
          <h2 className="text-xl font-bold text-white">Crear Nuevo Álbum</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-white transition-colors">
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {error && <div className="text-red-400 text-sm bg-red-500/10 p-2 rounded">{error}</div>}
          
          <FormField 
            label="Título del Álbum" 
            value={formData.title}
            onChange={(e) => setFormData({...formData, title: e.target.value})}
            required 
            maxLength={100}
            placeholder="Vacaciones 2026"
          />
          
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-gray-300">Descripción</label>
            <textarea 
              className="w-full bg-[#12141a] border border-[rgba(255,255,255,0.1)] focus:border-purple-500 focus:ring-1 focus:ring-purple-500 text-gray-100 rounded-lg px-4 py-2 text-sm placeholder-gray-500 transition-colors focus:outline-none resize-none h-24"
              value={formData.description}
              onChange={(e) => setFormData({...formData, description: e.target.value})}
              maxLength={500}
              placeholder="Opcional..."
            />
          </div>
          
          <div className="flex items-center gap-2 mt-1 mb-2">
            <input 
              type="checkbox" 
              id="isPublic"
              checked={formData.isPublic}
              onChange={(e) => setFormData({...formData, isPublic: e.target.checked})}
              className="rounded border-gray-600 bg-[#12141a] text-purple-600 focus:ring-purple-500 focus:ring-offset-[#1a1c26]"
            />
            <label htmlFor="isPublic" className="text-sm text-gray-300 cursor-pointer">
              Álbum Público (requiere aprobación)
            </label>
          </div>
          
          <div className="flex gap-3 justify-end mt-2">
            <Button type="button" variant="ghost" onClick={onClose}>Cancelar</Button>
            <Button type="submit" loading={loading}>Crear Álbum</Button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreateAlbumModal;
