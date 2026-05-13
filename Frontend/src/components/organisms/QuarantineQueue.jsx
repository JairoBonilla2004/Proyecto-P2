import React from 'react';
import QuarantineCard from '../molecules/QuarantineCard';

const QuarantineQueue = ({ queue, loading, onApprove, onReject, processingId }) => {
  if (loading) {
    return (
      <div className="flex flex-col gap-4">
        {[1, 2].map(i => <div key={i} className="h-64 glass-card skeleton"></div>)}
      </div>
    );
  }

  if (!queue || queue.length === 0) {
    return (
      <div className="glass-card p-10 text-center border-dashed border-white/10">
        <div className="w-16 h-16 bg-emerald-500/10 rounded-full flex items-center justify-center mx-auto mb-4">
          <svg className="w-8 h-8 text-emerald-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
          </svg>
        </div>
        <h3 className="text-lg font-medium text-white">Cola Limpia</h3>
        <p className="text-gray-400 mt-2">No hay imágenes en cuarentena pendientes de revisión.</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      {queue.map(item => (
        <QuarantineCard 
          key={item.id} 
          item={item} 
          onApprove={onApprove} 
          onReject={onReject} 
          isProcessing={processingId} 
        />
      ))}
    </div>
  );
};

export default QuarantineQueue;
