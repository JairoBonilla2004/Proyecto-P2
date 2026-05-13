import React, { useState } from 'react';
import ImageCard from '../molecules/ImageCard';

const ImageGrid = ({ images, loading }) => {
  const [selectedImage, setSelectedImage] = useState(null);
  if (loading) {
    return (
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        {[1, 2, 3, 4, 5, 6, 7, 8].map((i) => (
          <div key={i} className="glass-card aspect-[4/3] skeleton"></div>
        ))}
      </div>
    );
  }

  if (!images || images.length === 0) {
    return (
      <div className="text-center py-16 bg-white/5 rounded-xl border border-white/10 border-dashed">
        <svg className="mx-auto h-10 w-10 text-gray-600 mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
        <h3 className="text-base font-medium text-gray-400">Este álbum está vacío</h3>
        <p className="text-sm text-gray-500 mt-1">Sube la primera imagen para empezar.</p>
      </div>
    );
  }

  return (
    <>
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        {images.map((image) => (
          <ImageCard 
            key={image.id} 
            image={image} 
            onClick={() => image.imageStatus !== 'REJECTED' && setSelectedImage(image)} 
          />
        ))}
      </div>

      {/* Lightbox / Image Viewer Modal */}
      {selectedImage && (
        <div 
          className="fixed inset-0 z-[100] flex items-center justify-center p-4 md:p-10 animate-in fade-in duration-300"
          style={{ backgroundColor: 'rgba(3, 3, 5, 0.95)' }}
        >
          <button 
            onClick={() => setSelectedImage(null)}
            className="absolute top-6 right-6 text-white/50 hover:text-white transition-colors z-[110]"
          >
            <svg className="w-8 h-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>

          <div 
            className="absolute inset-0 z-0" 
            onClick={() => setSelectedImage(null)}
          ></div>

          <div className="relative z-10 max-w-5xl w-full h-full flex flex-col items-center justify-center animate-in zoom-in-95 duration-300">
            <div className="glass-card p-1 bg-white/5 border-white/10 shadow-2xl rounded-2xl overflow-hidden max-h-[85vh]">
              <img 
                src={selectedImage.storedUrl} 
                alt={selectedImage.originalName}
                className="max-w-full max-h-[80vh] object-contain rounded-xl"
              />
            </div>
            
            <div className="mt-6 text-center">
              <h3 className="text-xl font-bold text-white mb-1">{selectedImage.originalName}</h3>
              <div className="flex items-center gap-4 justify-center">
                <span className="text-sm text-gray-400">Subida el {new Date(selectedImage.uploadedAt).toLocaleDateString()}</span>
                <a 
                  href={selectedImage.storedUrl} 
                  target="_blank" 
                  rel="noreferrer"
                  className="text-sm font-bold text-[#00f0ff] hover:underline flex items-center gap-1"
                >
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                  </svg>
                  Ver original
                </a>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default ImageGrid;
