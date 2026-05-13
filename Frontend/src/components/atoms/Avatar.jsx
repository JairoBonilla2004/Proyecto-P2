import React from 'react';

const Avatar = ({ email, className = '' }) => {
  const initial = email ? email.charAt(0).toUpperCase() : '?';
  
  return (
    <div className={`flex items-center justify-center bg-gradient-to-br from-purple-500 to-indigo-600 text-white font-semibold rounded-full w-9 h-9 ${className}`}>
      {initial}
    </div>
  );
};

export default Avatar;
