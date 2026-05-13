import React from 'react';
import { Link } from 'react-router-dom';
import RegisterForm from '../components/organisms/RegisterForm';
import Logo from '../components/atoms/Logo';

const RegisterPage = () => {
  return (
    <div className="min-h-[80vh] flex flex-col items-center justify-center py-12 px-4 sm:px-6 lg:px-8 fade-in">
      <div className="w-full max-w-md space-y-8 glass-card p-8 shadow-2xl">
        <div className="flex flex-col items-center justify-center">
          <Logo className="mb-6 scale-125" />
          <h2 className="text-center text-3xl font-extrabold text-white">
            Crear cuenta
          </h2>
          <p className="mt-2 text-center text-sm text-gray-400">
            ¿Ya tienes cuenta?{' '}
            <Link to="/login" className="font-medium text-purple-400 hover:text-purple-300">
              Inicia sesión aquí
            </Link>
          </p>
        </div>
        
        <RegisterForm />
      </div>
    </div>
  );
};

export default RegisterPage;
