import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import FormField from '../molecules/FormField';
import Button from '../atoms/Button';
import { useAuth } from '../../context/AuthContext';

const RegisterForm = () => {
  const [formData, setFormData] = useState({ username: '', email: '', password: '' });
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [loading, setLoading] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

  const validate = () => {
    const newErrors = {};
    if (!formData.username || formData.username.length < 3) newErrors.username = 'El username debe tener al menos 3 caracteres.';
    if (!/^[a-zA-Z0-9_.-]+$/.test(formData.username)) newErrors.username = 'Solo letras, números, guiones y puntos.';
    if (!formData.email) newErrors.email = 'El correo es obligatorio.';
    if (!formData.password) newErrors.password = 'La contraseña es obligatoria.';
    // Regex from backend
    if (formData.password && !/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/.test(formData.password)) {
      newErrors.password = 'Mínimo 8 caracteres, una mayúscula, minúscula, número y carácter especial.';
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setApiError('');
    if (!validate()) return;
    
    setLoading(true);
    try {
      await register(formData);
      navigate('/login', { state: { message: 'Registro exitoso. Ahora puedes iniciar sesión.' } });
    } catch (err) {
      setApiError(err.response?.data?.message || 'Error al registrar usuario.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-5 w-full">
      {apiError && (
        <div className="p-3 bg-red-500/10 border border-red-500/50 rounded-lg text-red-400 text-sm">
          {apiError}
        </div>
      )}
      
      <FormField 
        label="Nombre de usuario" 
        value={formData.username}
        onChange={(e) => setFormData({...formData, username: e.target.value})}
        error={errors.username}
        placeholder="johndoe"
      />

      <FormField 
        label="Correo electrónico" 
        type="email" 
        value={formData.email}
        onChange={(e) => setFormData({...formData, email: e.target.value})}
        error={errors.email}
        placeholder="ejemplo@espe.edu.ec"
      />
      
      <FormField 
        label="Contraseña" 
        type="password" 
        value={formData.password}
        onChange={(e) => setFormData({...formData, password: e.target.value})}
        error={errors.password}
        placeholder="••••••••"
      />
      
      <Button type="submit" loading={loading} className="mt-2 w-full">
        Crear Cuenta
      </Button>
    </form>
  );
};

export default RegisterForm;
