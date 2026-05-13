import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import Logo from '../atoms/Logo';
import Avatar from '../atoms/Avatar';
import { useAuth } from '../../context/AuthContext';

const Navbar = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [scrolled, setScrolled] = useState(false);
  const [showDropdown, setShowDropdown] = useState(false);

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const isActive = (path) => location.pathname === path;

  return (
    <div className={`fixed top-0 inset-x-0 z-50 transition-all duration-300 ${scrolled ? 'py-4' : 'py-6'}`}>
      <nav className={`mx-auto max-w-7xl transition-all duration-500 px-4 sm:px-6 lg:px-8 ${scrolled ? 'w-[calc(100%-2rem)] md:w-full' : 'w-full'}`}>
        <div className={`flex items-center justify-between h-20 px-6 rounded-2xl transition-all duration-500 ${scrolled ? 'glass-card border border-white/10 shadow-2xl bg-[#030305]/80' : 'bg-transparent'}`}>
          <div className="flex items-center gap-10">
            <Logo />
            <div className="hidden md:flex gap-2">
              <Link to="/" className={`px-5 py-2.5 rounded-full text-sm font-bold transition-all ${isActive('/') ? 'text-white bg-white/10 shadow-inner' : 'text-gray-400 hover:text-white hover:bg-white/5'}`}>
                Explorar
              </Link>
              {user && (
                <Link to="/dashboard" className={`px-5 py-2.5 rounded-full text-sm font-bold transition-all ${isActive('/dashboard') ? 'text-white bg-white/10 shadow-inner' : 'text-gray-400 hover:text-white hover:bg-white/5'}`}>
                  Mis Colecciones
                </Link>
              )}
              {user?.role === 'ROLE_SUPERVISOR' && (
                <Link to="/supervisor" className={`px-5 py-2.5 rounded-full text-sm font-bold transition-all ${location.pathname.startsWith('/supervisor') ? 'text-[#00f0ff] bg-[#00f0ff]/10 shadow-[0_0_15px_rgba(0,240,255,0.1)]' : 'text-gray-400 hover:text-[#00f0ff] hover:bg-[#00f0ff]/10'}`}>
                  Centro de Control
                </Link>
              )}
            </div>
          </div>
          
          <div className="flex items-center gap-4">
            {user ? (
              <div className="relative pl-6 border-l border-white/10">
                <button 
                  onClick={() => setShowDropdown(!showDropdown)}
                  className="flex items-center gap-3 group focus:outline-none"
                >
                  <Avatar email={user.email} className={`ring-2 transition-all ${showDropdown ? 'ring-[#7000ff] shadow-[0_0_15px_rgba(112,0,255,0.4)]' : 'ring-white/10 group-hover:ring-white/30'}`} />
                  <svg className={`w-4 h-4 text-gray-500 transition-transform duration-300 ${showDropdown ? 'rotate-180' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M19 9l-7 7-7-7" />
                  </svg>
                </button>

                {showDropdown && (
                  <>
                    <div 
                      className="fixed inset-0 z-0" 
                      onClick={() => setShowDropdown(false)}
                    ></div>
                    <div className="absolute right-0 mt-4 w-64 glass-card border border-white/10 bg-[#0a0a0f]/95 backdrop-blur-2xl rounded-2xl shadow-2xl overflow-hidden z-50 animate-in fade-in zoom-in duration-200 origin-top-right">
                      <div className="p-4 border-b border-white/5 bg-white/5">
                        <p className="text-[10px] uppercase tracking-widest text-gray-500 font-black mb-1">Usuario</p>
                        <p className="text-sm font-bold text-white truncate">{user.email}</p>
                        <p className="text-[10px] text-[#7000ff] font-bold mt-1 uppercase tracking-tighter">
                          {user.role === 'ROLE_SUPERVISOR' ? 'Administrador' : 'Miembro'}
                        </p>
                      </div>
                      <div className="p-2">
                        <button 
                          onClick={handleLogout}
                          className="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-bold text-gray-400 hover:text-[#ff0055] hover:bg-[#ff0055]/10 transition-all group"
                        >
                          <svg className="w-5 h-5 transition-transform group-hover:translate-x-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                          </svg>
                          Cerrar Sesión
                        </button>
                      </div>
                    </div>
                  </>
                )}
              </div>
            ) : (
              <div className="flex items-center gap-4">
                <Link to="/login" className="text-sm font-bold text-gray-300 hover:text-white transition-colors">
                  Iniciar Sesión
                </Link>
                <Link to="/register" className="btn-premium py-2.5 px-6 text-sm">
                  Empezar Ahora
                </Link>
              </div>
            )}
          </div>
        </div>
      </nav>
    </div>
  );
};

export default Navbar;
