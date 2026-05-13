import React from 'react';
import { Outlet } from 'react-router-dom';
import Navbar from '../organisms/Navbar';

const PublicLayout = () => {
  return (
    <div className="min-h-screen flex flex-col relative z-0">
      <Navbar />
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 pt-32 pb-12">
        <Outlet />
      </main>
      <footer className="border-t border-white/5 bg-[#030305]/50 backdrop-blur-md py-8 mt-auto z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col md:flex-row justify-between items-center text-sm text-gray-500 font-medium">
          <p>© 2026 SecureFrame</p>
          <p className="mt-2 md:mt-0 font-display tracking-wider uppercase text-xs font-bold text-gray-600">Ingeniería de Software Segura</p>
        </div>
      </footer>
    </div>
  );
};

export default PublicLayout;
