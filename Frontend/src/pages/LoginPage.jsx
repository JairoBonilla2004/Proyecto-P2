import React from 'react';
import { Link } from 'react-router-dom';
import LoginForm from '../components/organisms/LoginForm';
import Logo from '../components/atoms/Logo';

const LoginPage = () => {
  return (
    <div className="min-h-[80vh] flex flex-col items-center justify-center py-12 px-4 sm:px-6 lg:px-8 fade-in">
      <div className="w-full max-w-md space-y-8 glass-card p-8 shadow-2xl">
        <div className="flex flex-col items-center justify-center">
          <Logo className="mb-6 scale-125" />
          <h2 className="text-center text-3xl font-extrabold text-white">
            Iniciar sesión
          </h2>
          <p className="mt-2 text-center text-sm text-gray-400">
            ¿No tienes cuenta?{' '}
            <Link to="/register" className="font-medium text-purple-400 hover:text-purple-300">
              Regístrate aquí
            </Link>
          </p>
        </div>
        
        <LoginForm />
      </div>
    </div>
  );
};

export default LoginPage;
