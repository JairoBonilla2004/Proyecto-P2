import React, { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

const OAuth2RedirectHandler = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { loginWithToken } = useAuth();
  const [errorMsg, setErrorMsg] = useState('');
  const [status, setStatus] = useState('Procesando token...');

  useEffect(() => {
    const processToken = () => {
      const queryParams = new URLSearchParams(location.search);
      const token = queryParams.get('token');

      if (!token) {
        setErrorMsg('No se encontró el token en la URL.');
        return;
      }

      try {
        setStatus('Iniciando sesión...');
        loginWithToken(token);
        setStatus('¡Éxito! Redirigiendo...');
        // Small timeout to ensure state is set before navigating
        setTimeout(() => {
          navigate('/dashboard', { replace: true });
        }, 500);
      } catch (error) {
        console.error('Failed to process OAuth2 token', error);
        setErrorMsg('Error al procesar el token: ' + error.message);
      }
    };

    processToken();
    // Intentionally removing loginWithToken and navigate from deps to run only ONCE
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [location.search]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-[#050505]">
      <div className="flex flex-col items-center gap-6 max-w-md text-center p-8 glass-card">
        {errorMsg ? (
          <>
            <div className="w-16 h-16 rounded-full bg-red-500/20 flex items-center justify-center text-red-500 mb-2">
              <svg className="w-8 h-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </div>
            <p className="text-red-400 font-semibold">{errorMsg}</p>
            <button 
              onClick={() => navigate('/login')}
              className="mt-4 px-6 py-2 bg-white/10 hover:bg-white/20 rounded-lg text-white transition-colors"
            >
              Volver al Login
            </button>
          </>
        ) : (
          <>
            <div className="w-12 h-12 border-4 border-[#00f0ff] border-t-transparent rounded-full animate-spin"></div>
            <p className="text-gray-400 font-semibold animate-pulse">{status}</p>
            <button 
              onClick={() => navigate('/dashboard')}
              className="mt-4 text-xs text-gray-500 hover:text-white underline"
            >
              ¿Atascado? Haz clic aquí para continuar
            </button>
          </>
        )}
      </div>
    </div>
  );
};

export default OAuth2RedirectHandler;
