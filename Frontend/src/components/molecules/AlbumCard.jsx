import React from 'react';
import { Link } from 'react-router-dom';
import Badge from '../atoms/Badge';

const AlbumCard = ({ album }) => {
  return (
    <Link 
      to={`/albums/${album.id}`}
      className="glass-card flex flex-col group relative overflow-hidden transition-all duration-500 hover:-translate-y-2 hover:shadow-[0_20px_40px_-15px_rgba(112,0,255,0.4)] border-white/5"
    >
      <div className="absolute inset-0 bg-gradient-to-br from-[#00f0ff]/0 via-[#7000ff]/0 to-[#ff0055]/0 group-hover:from-[#00f0ff]/10 group-hover:via-[#7000ff]/10 group-hover:to-[#ff0055]/10 transition-colors duration-500 z-0"></div>
      
      <div className="h-56 bg-[#0a0a0f] flex items-center justify-center relative overflow-hidden z-10 border-b border-white/5">
        {/* Tech Grid Pattern */}
        <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAiIGhlaWdodD0iMjAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGNpcmNsZSBjeD0iMiIgY3k9IjIiIHI9IjEiIGZpbGw9InJnYmEoMjU1LDI1NSwyNTUsMC4wNSkiLz48L3N2Zz4=')] opacity-50 group-hover:scale-110 transition-transform duration-1000"></div>
        <div className="absolute inset-0 bg-gradient-to-t from-[#12121a] to-transparent z-10"></div>
        
        <div className="relative z-20 w-20 h-20 rounded-2xl bg-white/5 border border-white/10 flex items-center justify-center backdrop-blur-xl group-hover:scale-110 transition-transform duration-500 group-hover:border-[#00f0ff]/50 shadow-[0_0_30px_rgba(0,240,255,0.15)] group-hover:shadow-[0_0_40px_rgba(112,0,255,0.3)]">
          <svg className="w-10 h-10 text-white/80 group-hover:text-[#00f0ff] transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
        </div>
        <div className="absolute top-4 right-4 z-20 flex flex-col gap-2 items-end">
          <Badge status={album.approvalStatus} />
          <span className={`text-[10px] uppercase font-bold tracking-wider px-2 py-1 rounded-md backdrop-blur-md border ${
            (album.public !== undefined ? album.public : album.isPublic)
              ? 'bg-blue-500/20 text-blue-300 border-blue-500/30' 
              : 'bg-gray-500/20 text-gray-300 border-gray-500/30'
          }`}>
            {(album.public !== undefined ? album.public : album.isPublic) ? 'Público' : 'Privado'}
          </span>
        </div>
      </div>
      
      <div className="p-6 flex flex-col gap-3 flex-grow relative z-10 bg-gradient-to-b from-[#12121a] to-[#0a0a0f]">
        <h3 className="font-display font-bold text-2xl text-white group-hover:text-[#00f0ff] transition-colors line-clamp-1">{album.title}</h3>
        <p className="text-[15px] text-gray-400 line-clamp-2 flex-grow leading-relaxed font-medium">{album.description || 'Sin descripción disponible'}</p>
        
        <div className="pt-5 mt-3 border-t border-white/5 flex justify-between items-center text-[13px] font-bold text-gray-500 uppercase tracking-widest">
          <span className="flex items-center gap-2 bg-white/5 px-3 py-1.5 rounded-lg text-white">
            <svg className="w-4 h-4 text-[#ff0055]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            {album.imageCount} items
          </span>
          <span>{new Date(album.createdAt).toLocaleDateString()}</span>
        </div>
      </div>
    </Link>
  );
};

export default AlbumCard;
