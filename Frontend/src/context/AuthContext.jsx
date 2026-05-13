import React, { createContext, useState, useEffect, useContext } from 'react';
import { authApi } from '../api/authApi';

const AuthContext = createContext();

export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const storedToken = localStorage.getItem('token');
    const storedUser = localStorage.getItem('user');
    
    if (storedToken && storedUser) {
      setToken(storedToken);
      setUser(JSON.parse(storedUser));
    }
    setLoading(false);
  }, []);

  const login = async (credentials) => {
    const res = await authApi.login(credentials);
    const token = res.data.accessToken;
    return loginWithToken(token);
  };

  const loginWithToken = (token) => {
    try {
      // Parse JWT to extract role and email
      const payload = JSON.parse(atob(token.split('.')[1]));
      // JWT standard uses sub for email/username, Spring Security might use 'role' or 'authorities'
      const role = payload.role || (payload.authorities && payload.authorities[0]) || 'ROLE_USER';
      const userObj = { email: payload.sub, role };

      localStorage.setItem('token', token);
      localStorage.setItem('user', JSON.stringify(userObj));
      
      setToken(token);
      setUser(userObj);
      return userObj;
    } catch (e) {
      console.error('Failed to parse token', e);
      throw new Error('Invalid token format');
    }
  };

  const register = async (userData) => {
    return await authApi.register(userData);
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, token, loading, login, loginWithToken, register, logout }}>
      {!loading && children}
    </AuthContext.Provider>
  );
};

export default AuthContext;
