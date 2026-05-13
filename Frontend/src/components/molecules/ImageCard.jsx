import React from 'react';
import Badge from '../atoms/Badge';

const ImageCard = ({ image, onClick }) => {
  return (
    <div 
      onClick={onClick}
      className="glass-card relative overflow-hidden group aspect-[4/3] bg-[#1a1c26] cursor-pointer"
    >
      {image.imageStatus === 'REJECTED' ? (
        <div className="w-full h-full flex flex-col items-center justify-center bg-red-950/20 text-red-500/50 p-4 text-center">
          <svg className="w-12 h-12 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
          </svg>
          <span className="text-[10px] uppercase font-black tracking-widest">Contenido Bloqueado</span>
          <span className="text-[9px] mt-1 text-red-500/30">Incumple políticas de seguridad</span>
        </div>
      ) : (
        <img 
          src={image.storedUrl} 
          alt={image.originalName}
          className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
          loading="lazy"
        />
      )}
      
      <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity flex flex-col justify-end p-4">
        <p className="text-sm font-medium text-white line-clamp-1">{image.originalName}</p>
        <p className="text-xs text-gray-300 mt-1">{new Date(image.uploadedAt).toLocaleDateString()}</p>
      </div>

      <div className="absolute top-2 right-2">
        <Badge status={image.imageStatus} />
      </div>
    </div>
  );
};

export default ImageCard;
