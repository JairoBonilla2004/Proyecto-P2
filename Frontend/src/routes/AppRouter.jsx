import React from 'react';
import { Routes, Route } from 'react-router-dom';
import PublicLayout from '../components/templates/PublicLayout';
import DashboardLayout from '../components/templates/DashboardLayout';

import GalleryPage from '../pages/GalleryPage';
import AlbumDetailPage from '../pages/AlbumDetailPage';
import LoginPage from '../pages/LoginPage';
import RegisterPage from '../pages/RegisterPage';
import DashboardPage from '../pages/DashboardPage';
import SupervisorDashboard from '../pages/supervisor/SupervisorDashboard';
import OAuth2RedirectHandler from '../components/organisms/OAuth2RedirectHandler';

const AppRouter = () => {
  return (
    <Routes>
      <Route element={<PublicLayout />}>
        <Route path="/" element={<GalleryPage />} />
        <Route path="/albums/:id" element={<AlbumDetailPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/oauth2/redirect" element={<OAuth2RedirectHandler />} />
      </Route>

      <Route element={<DashboardLayout />}>
        <Route path="/dashboard" element={<DashboardPage />} />
      </Route>

      <Route element={<DashboardLayout requireSupervisor={true} />}>
        <Route path="/supervisor" element={<SupervisorDashboard />} />
      </Route>
    </Routes>
  );
};

export default AppRouter;
