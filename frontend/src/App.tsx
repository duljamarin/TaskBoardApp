import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Login, Register, AuthGuard, AdminGuard } from './components/auth';
import { BoardList, BoardView } from './components/boards';
import { AnalyticsDashboard } from './components/analytics';
import { AdminPanel } from './components/admin';
import { Layout } from './components/layout';
import { Dashboard } from './components/Dashboard';

const App: React.FC = () => {
  return (
    <Router>
      <Routes>
        {/* Public routes */}
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        {/* Protected routes */}
        <Route
          path="/boards"
          element={
            <AuthGuard>
              <Layout>
                <BoardList />
              </Layout>
            </AuthGuard>
          }
        />
        <Route
          path="/boards/:id"
          element={
            <AuthGuard>
              <BoardView />
            </AuthGuard>
          }
        />
        <Route
          path="/analytics"
          element={
            <AuthGuard>
              <Layout>
                <AnalyticsDashboard />
              </Layout>
            </AuthGuard>
          }
        />

        {/* Admin routes */}
        <Route
          path="/admin"
          element={
            <AdminGuard>
              <Layout>
                <AdminPanel />
              </Layout>
            </AdminGuard>
          }
        />

        {/* Default routes */}
        <Route path="/" element={<Dashboard />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Router>
  );
};

export default App;

