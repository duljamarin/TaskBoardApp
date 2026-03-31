import React, { useEffect } from 'react';
import { Navbar } from './Navbar';

interface LayoutProps {
  children: React.ReactNode;
}

export const Layout: React.FC<LayoutProps> = ({ children }) => {
  // Reset any stuck body styles (e.g. from modals that didn't clean up)
  useEffect(() => {
    document.body.style.overflow = '';
    document.body.style.pointerEvents = '';
  }, []);

  return (
    <div className="min-h-screen bg-surface-50">
      <Navbar />
      <main className="relative">{children}</main>
    </div>
  );
};

