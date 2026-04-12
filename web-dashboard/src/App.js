import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import LandingPage from './pages/LandingPage';
import { ToastContainer } from './components/Toast';

function PrivateRoute({ children }) {
  const { user } = useAuth();
  return user ? children : <Navigate to="/login" replace />;
}

function PublicRoute({ children }) {
  const { user } = useAuth();
  return !user ? children : <Navigate to="/dashboard" replace />;
}

export default function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/"         element={<LandingPage />} />
            <Route path="/login"     element={<PublicRoute><LoginPage /></PublicRoute>} />
            <Route path="/dashboard" element={<PrivateRoute><DashboardPage /></PrivateRoute>} />
            <Route path="*"          element={<Navigate to="/" replace />} />
          </Routes>
        </BrowserRouter>
        <ToastContainer />
      </AuthProvider>
    </ThemeProvider>
  );
}
