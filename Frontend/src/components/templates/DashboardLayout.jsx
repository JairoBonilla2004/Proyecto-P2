import React from 'react';
import { Outlet, Navigate, Link, useLocation } from 'react-router-dom';
import Navbar from '../organisms/Navbar';
import { useAuth } from '../../context/AuthContext';

const DashboardLayout = ({ requireSupervisor = false }) => {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) return null;

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (requireSupervisor && user.role !== 'ROLE_SUPERVISOR') {
    return <Navigate to="/dashboard" replace />;
  }

  return (
    <div className="min-h-screen flex flex-col relative z-0">
      <div className="bg-mesh"></div>
      <Navbar />
      <div className="flex-1 flex max-w-7xl w-full mx-auto pt-36 pb-12 relative z-10 px-4 sm:px-6 lg:px-8">
        {requireSupervisor && (
          <aside className="w-72 border-r border-white/5 hidden md:block pr-8 mr-8">
            <div className="glass-card p-6 sticky top-32 bg-[#0a0a0f]/80">
              <h3 className="text-xs font-bold text-[#00f0ff] uppercase tracking-widest mb-6 font-display">Administración</h3>
              <nav className="flex flex-col gap-2">
                <Link 
                  to="/supervisor" 
                  className={`px-4 py-3 text-sm font-semibold rounded-xl flex items-center gap-3 transition-all ${location.pathname === '/supervisor' ? 'bg-[#7000ff]/20 text-white border border-[#7000ff]/30 shadow-[0_0_15px_rgba(112,0,255,0.2)]' : 'text-gray-400 hover:bg-white/5 hover:text-white'}`}
                >
                  <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                  </svg>
                  Panel de Control
                </Link>
              </nav>
            </div>
          </aside>
        )}
        <main className="flex-1">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default DashboardLayout;
