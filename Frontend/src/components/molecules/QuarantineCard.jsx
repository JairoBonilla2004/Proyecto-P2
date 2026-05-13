import React from 'react';
import Button from '../atoms/Button';
import Badge from '../atoms/Badge';

const QuarantineCard = ({ item, onApprove, onReject, isProcessing }) => {
  return (
    <div className="glass-card flex flex-col md:flex-row overflow-hidden border border-orange-500/30">
      <div className="md:w-64 bg-[#1a1c26] relative">
        <img 
          src={item.imageUrl} 
          alt={item.imageName}
          className="w-full h-full object-cover"
        />
        <div className="absolute top-2 right-2">
          <Badge status="QUARANTINED" />
        </div>
      </div>
      
      <div className="p-5 flex-1 flex flex-col">
        <div className="flex justify-between items-start mb-4">
          <div>
            <h3 className="text-lg font-semibold text-gray-100">{item.imageName}</h3>
            <p className="text-sm text-gray-400">ID Imagen: {item.imageId}</p>
          </div>
          <span className="text-xs text-gray-500">{new Date(item.createdAt).toLocaleString()}</span>
        </div>

        <div className="bg-[#12141a] rounded p-4 border border-gray-800 mb-4 flex-grow">
          <h4 className="text-sm font-medium text-orange-400 mb-2 flex items-center gap-2">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            Análisis de Detección
          </h4>
          <p className="text-sm text-gray-300 whitespace-pre-wrap">{item.detectionReason}</p>
          <div className="mt-3 grid grid-cols-2 gap-4 text-sm">
            <div>
              <span className="text-gray-500 block text-xs">Entropía LSB</span>
              <span className={`font-mono ${item.lsbScore > 0.95 ? 'text-red-400' : 'text-gray-300'}`}>
                {item.lsbScore ? item.lsbScore.toFixed(4) : 'N/A'}
              </span>
            </div>
            <div>
              <span className="text-gray-500 block text-xs">Anomalía EOF</span>
              <span className={`font-medium ${item.eofAnomaly ? 'text-red-400' : 'text-emerald-400'}`}>
                {item.eofAnomaly ? 'Detectado' : 'Limpio'}
              </span>
            </div>
          </div>
        </div>

        <div className="flex gap-3 justify-end mt-auto">
          <Button 
            variant="danger" 
            onClick={() => onReject(item.imageId)}
            loading={isProcessing === item.imageId}
          >
            Rechazar (Borrar)
          </Button>
          <Button 
            variant="primary" 
            onClick={() => onApprove(item.imageId)}
            loading={isProcessing === item.imageId}
            className="bg-emerald-600 hover:bg-emerald-700 shadow-none focus:ring-emerald-500"
          >
            Falso Positivo (Aprobar)
          </Button>
        </div>
      </div>
    </div>
  );
};

export default QuarantineCard;
