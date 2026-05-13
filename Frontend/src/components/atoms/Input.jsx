import React from 'react';

const Input = React.forwardRef(({ className = '', error, ...props }, ref) => {
  return (
    <input
      ref={ref}
      className={`w-full bg-[#12141a] border ${error ? 'border-red-500 focus:ring-red-500' : 'border-[rgba(255,255,255,0.1)] focus:border-purple-500 focus:ring-purple-500'} text-gray-100 rounded-lg px-4 py-2 text-sm placeholder-gray-500 transition-colors focus:outline-none focus:ring-1 ${className}`}
      {...props}
    />
  );
});

Input.displayName = 'Input';
export default Input;
