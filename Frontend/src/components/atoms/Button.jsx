import React from 'react';

const Button = ({ children, variant = 'primary', size = 'md', className = '', loading = false, ...props }) => {
  const baseStyle = 'inline-flex items-center justify-center font-bold rounded-full transition-all duration-300 focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed font-sans tracking-wide';
  
  const variants = {
    primary: 'bg-white text-black hover:bg-gray-200 shadow-[0_0_20px_rgba(255,255,255,0.3)]',
    secondary: 'bg-[#12121a] hover:bg-[#1a1a24] text-white border border-white/10 hover:border-white/20',
    danger: 'bg-[#ff0055]/10 text-[#ff0055] hover:bg-[#ff0055]/20 border border-[#ff0055]/30',
    ghost: 'bg-transparent hover:bg-white/5 text-gray-300 hover:text-white',
    premium: 'btn-premium'
  };

  const sizes = {
    sm: 'text-sm px-5 py-2.5',
    md: 'text-[15px] px-8 py-3.5',
    lg: 'text-lg px-10 py-4',
  };

  return (
    <button 
      className={`${baseStyle} ${variants[variant]} ${sizes[size]} ${className}`}
      disabled={loading}
      {...props}
    >
      {loading && (
        <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-current" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
      )}
      {children}
    </button>
  );
};

export default Button;
