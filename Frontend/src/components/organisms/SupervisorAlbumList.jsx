import React from 'react';
import Button from '../atoms/Button';
import Badge from '../atoms/Badge';

const SupervisorAlbumList = ({ albums, loading, onApprove, onReject, processingId }) => {
  if (loading) {
    return (
      <div className="flex flex-col gap-3">
        {[1, 2, 3].map(i => <div key={i} className="h-24 glass-card skeleton"></div>)}
      </div>
    );
  }

  if (!albums || albums.length === 0) {
    return (
      <div className="glass-card p-8 text-center border-dashed border-white/10">
        <p className="text-gray-400">No hay álbumes pendientes de revisión.</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      {albums.map(album => (
        <div key={album.id} className="glass-card p-4 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
          <div>
            <div className="flex items-center gap-3 mb-1">
              <h4 className="text-lg font-semibold text-white">{album.title}</h4>
              <Badge status={album.approvalStatus} />
            </div>
            <p className="text-sm text-gray-400 mb-2">{album.description}</p>
            <p className="text-xs text-gray-500">Solicitado por: <span className="text-gray-300">{album.ownerEmail}</span> • {new Date(album.createdAt).toLocaleString()}</p>
          </div>
          
          <div className="flex gap-2 w-full md:w-auto">
            <Button 
              variant="danger" 
              size="sm" 
              onClick={() => onReject(album.id)}
              loading={processingId === album.id}
              className="flex-1 md:flex-none"
            >
              Rechazar
            </Button>
            <Button 
              variant="primary" 
              size="sm" 
              onClick={() => onApprove(album.id)}
              loading={processingId === album.id}
              className="flex-1 md:flex-none bg-emerald-600 hover:bg-emerald-700 focus:ring-emerald-500 shadow-none"
            >
              Aprobar
            </Button>
          </div>
        </div>
      ))}
    </div>
  );
};

export default SupervisorAlbumList;
