import React from 'react';
import { Link } from 'react-router-dom';

const Logo = ({ className = '' }) => {
  return (
    <Link to="/" className={`flex items-center gap-3 group ${className}`}>
      <div className="w-12 h-12 rounded-[14px] bg-gradient-to-br from-[#00f0ff] via-[#7000ff] to-[#ff0055] flex items-center justify-center shadow-[0_0_20px_rgba(112,0,255,0.4)] group-hover:shadow-[0_0_30px_rgba(0,240,255,0.6)] transition-all duration-500 relative overflow-hidden">
        <div className="absolute inset-0 bg-white/20 blur-md transform scale-150 group-hover:rotate-180 transition-transform duration-1000"></div>
        <svg className="w-6 h-6 text-white relative z-10 drop-shadow-md" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
        </svg>
      </div>
      <span className="font-display font-extrabold text-2xl tracking-tight text-white flex items-center">
        Secure<span className="text-gradient">Frame</span>
      </span>
    </Link>
  );
};

export default Logo;
