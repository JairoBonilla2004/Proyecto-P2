import React from 'react';

const Badge = ({ status, children, className = '' }) => {
  const statusStyles = {
    CLEAN: 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20',
    APPROVED: 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20',
    SUSPICIOUS: 'bg-amber-500/10 text-amber-400 border border-amber-500/20',
    QUARANTINED: 'bg-orange-500/10 text-orange-400 border border-orange-500/20',
    POSITIVE: 'bg-red-500/10 text-red-400 border border-red-500/20',
    REJECTED: 'bg-red-500/10 text-red-400 border border-red-500/20',
    PENDING: 'bg-gray-500/10 text-gray-400 border border-gray-500/20',
    PENDING_REVIEW: 'bg-gray-500/10 text-gray-400 border border-gray-500/20',
  };

  const style = statusStyles[status] || statusStyles.PENDING;

  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${style} ${className}`}>
      {children || status}
    </span>
  );
};

export default Badge;
