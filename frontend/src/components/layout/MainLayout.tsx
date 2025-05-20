import React, { useState, useEffect } from 'react';
import Header from './Header';
import Sidebar from './Sidebar';
import './MainLayout.css';

export interface BreadcrumbItem {
  label: string;
  path?: string;
}

export interface MainLayoutProps {
  children: React.ReactNode;
  title?: string;
  breadcrumbs?: BreadcrumbItem[];
}

const MainLayout: React.FC<MainLayoutProps> = ({
  children,
  title,
  breadcrumbs = []
}) => {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [isMobile, setIsMobile] = useState(false);
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false);
  
  // Handle responsive behavior
  useEffect(() => {
    const handleResize = () => {
      setIsMobile(window.innerWidth < 992);
    };
    
    // Initial check
    handleResize();
    
    // Add resize listener
    window.addEventListener('resize', handleResize);
    
    // Clean up
    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, []);
  
  // Toggle sidebar on desktop
  const toggleSidebar = () => {
    setSidebarCollapsed(!sidebarCollapsed);
  };
  
  // Toggle sidebar on mobile
  const toggleMobileSidebar = () => {
    setMobileSidebarOpen(!mobileSidebarOpen);
  };
  
  // Close mobile sidebar when clicking outside
  const handleContentClick = () => {
    if (isMobile && mobileSidebarOpen) {
      setMobileSidebarOpen(false);
    }
  };
  
  // Layout classes
  const layoutClasses = [
    'main-layout',
    sidebarCollapsed ? 'sidebar-collapsed' : '',
    isMobile ? 'mobile-layout' : '',
    mobileSidebarOpen ? 'mobile-sidebar-open' : ''
  ].filter(Boolean).join(' ');
  
  return (
    <div className={layoutClasses}>
      <Header 
        onToggleSidebar={isMobile ? toggleMobileSidebar : toggleSidebar}
        isMobile={isMobile}
        sidebarCollapsed={isMobile ? !mobileSidebarOpen : sidebarCollapsed}
      />
      
      <Sidebar 
        collapsed={isMobile ? !mobileSidebarOpen : sidebarCollapsed}
        isMobile={isMobile}
      />
      
      <main className="main-content" onClick={handleContentClick}>
        <div className="content-wrapper">
          {/* Page header with title and breadcrumbs */}
          {(title || breadcrumbs.length > 0) && (
            <div className="page-header">
              {title && <h1 className="page-title">{title}</h1>}
              
              {breadcrumbs.length > 0 && (
                <nav className="breadcrumbs" aria-label="breadcrumbs">
                  <ol className="breadcrumbs-list">
                    {breadcrumbs.map((item, index) => (
                      <li key={index} className="breadcrumbs-item">
                        {index < breadcrumbs.length - 1 && item.path ? (
                          <a href={item.path} className="breadcrumbs-link">
                            {item.label}
                          </a>
                        ) : (
                          <span className="breadcrumbs-text">{item.label}</span>
                        )}
                        {index < breadcrumbs.length - 1 && (
                          <span className="breadcrumbs-separator">/</span>
                        )}
                      </li>
                    ))}
                  </ol>
                </nav>
              )}
            </div>
          )}
          
          {/* Main content */}
          <div className="content-container">
            {children}
          </div>
        </div>
      </main>
      
      {/* Overlay for mobile sidebar */}
      {isMobile && mobileSidebarOpen && (
        <div className="sidebar-overlay" onClick={toggleMobileSidebar} />
      )}
    </div>
  );
};

export default MainLayout; 