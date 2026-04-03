import React, { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);

  useEffect(() => {
    const token = localStorage.getItem('jwt_token');
    const role = localStorage.getItem('role');
    const userId = localStorage.getItem('user_id');
    const fullName = localStorage.getItem('full_name');
    if (token) setUser({ token, role, userId: Number(userId), fullName });
  }, []);

  const login = (authResponse) => {
    localStorage.setItem('jwt_token', authResponse.token);
    localStorage.setItem('role', authResponse.role);
    localStorage.setItem('user_id', authResponse.userId);
    localStorage.setItem('full_name', authResponse.fullName);
    setUser(authResponse);
  };

  const logout = () => {
    localStorage.clear();
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
