import React, { useState, useEffect } from 'react';
import Card from '../common/Card';
import Button from '../common/Button';
import Spinner from '../common/Spinner';
import Alert from '../common/Alert';
import notificationService, { Notification } from '../../services/notificationService';
import './Notifications.css';

// Notification types
export enum NotificationType {
  INFO = 'info',
  SUCCESS = 'success',
  WARNING = 'warning',
  ERROR = 'error'
}

/**
 * NotificationsPanel component for displaying and managing notifications
 */
const NotificationsPanel: React.FC = () => {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedFilter, setSelectedFilter] = useState<string>('all');
  
  // Load notifications when component mounts
  useEffect(() => {
    const fetchNotifications = async () => {
      setIsLoading(true);
      setError(null);
      
      try {
        // Fetch notifications using service
        const data = await notificationService.getNotifications();
        setNotifications(data);
      } catch (err) {
        console.error('Error loading notifications:', err);
        setError(err instanceof Error ? err.message : 'Failed to load notifications');
      } finally {
        setIsLoading(false);
      }
    };
    
    fetchNotifications();
  }, []);
  
  // Handle notification mark as read
  const handleMarkAsRead = async (id: string) => {
    try {
      // Update notification status using service
      await notificationService.markAsRead(id);
      
      // Update local state
      setNotifications(prev => 
        prev.map(notification => 
          notification.id === id 
            ? { ...notification, isRead: true } 
            : notification
        )
      );
    } catch (err) {
      console.error('Error updating notification:', err);
      setError(err instanceof Error ? err.message : 'Failed to update notification');
    }
  };
  
  // Handle mark all as read
  const handleMarkAllAsRead = async () => {
    try {
      // Update all notifications using service
      await notificationService.markAllAsRead();
      
      // Update local state
      setNotifications(prev => 
        prev.map(notification => ({ ...notification, isRead: true }))
      );
    } catch (err) {
      console.error('Error updating notifications:', err);
      setError(err instanceof Error ? err.message : 'Failed to update all notifications');
    }
  };
  
  // Handle notification delete
  const handleDelete = async (id: string) => {
    try {
      // Delete notification using service
      await notificationService.deleteNotification(id);
      
      // Update local state
      setNotifications(prev => 
        prev.filter(notification => notification.id !== id)
      );
    } catch (err) {
      console.error('Error deleting notification:', err);
      setError(err instanceof Error ? err.message : 'Failed to delete notification');
    }
  };
  
  // Handle clear all notifications
  const handleClearAll = async () => {
    try {
      // Delete all notifications using service
      await notificationService.deleteAllNotifications();
      
      // Update local state
      setNotifications([]);
    } catch (err) {
      console.error('Error clearing notifications:', err);
      setError(err instanceof Error ? err.message : 'Failed to clear all notifications');
    }
  };
  
  // Filter notifications
  const filteredNotifications = selectedFilter === 'all' 
    ? notifications 
    : selectedFilter === 'unread' 
      ? notifications.filter(n => !n.isRead) 
      : notifications.filter(n => n.type === selectedFilter);
  
  // Format timestamp for display
  const formatTimestamp = (timestamp: string) => {
    const date = new Date(timestamp);
    return date.toLocaleString();
  };
  
  if (isLoading) {
    return <Spinner text="Loading notifications..." />;
  }
  
  return (
    <div className="notifications-container">
      <h1 className="page-title">Notifications</h1>
      
      {error && (
        <Alert 
          type="error" 
          message={error} 
          onClose={() => setError(null)} 
        />
      )}
      
      <div className="notifications-header">
        <div className="notification-filters">
          <button 
            className={`filter-button ${selectedFilter === 'all' ? 'active' : ''}`}
            onClick={() => setSelectedFilter('all')}
          >
            All
          </button>
          <button 
            className={`filter-button ${selectedFilter === 'unread' ? 'active' : ''}`}
            onClick={() => setSelectedFilter('unread')}
          >
            Unread
          </button>
          <button 
            className={`filter-button ${selectedFilter === NotificationType.INFO ? 'active' : ''}`}
            onClick={() => setSelectedFilter(NotificationType.INFO)}
          >
            Info
          </button>
          <button 
            className={`filter-button ${selectedFilter === NotificationType.SUCCESS ? 'active' : ''}`}
            onClick={() => setSelectedFilter(NotificationType.SUCCESS)}
          >
            Success
          </button>
          <button 
            className={`filter-button ${selectedFilter === NotificationType.WARNING ? 'active' : ''}`}
            onClick={() => setSelectedFilter(NotificationType.WARNING)}
          >
            Warning
          </button>
          <button 
            className={`filter-button ${selectedFilter === NotificationType.ERROR ? 'active' : ''}`}
            onClick={() => setSelectedFilter(NotificationType.ERROR)}
          >
            Error
          </button>
        </div>
        
        <div className="notification-actions">
          <Button 
            variant="secondary" 
            onClick={handleMarkAllAsRead}
            disabled={notifications.every(n => n.isRead)}
          >
            Mark All as Read
          </Button>
          <Button 
            variant="secondary" 
            onClick={handleClearAll}
            disabled={notifications.length === 0}
          >
            Clear All
          </Button>
        </div>
      </div>
      
      <div className="notifications-list">
        {filteredNotifications.length === 0 ? (
          <Card className="empty-state">
            <div className="empty-state-content">
              <h3>No notifications</h3>
              <p>You don't have any {selectedFilter !== 'all' ? selectedFilter : ''} notifications at this time.</p>
            </div>
          </Card>
        ) : (
          filteredNotifications.map(notification => (
            <Card 
              key={notification.id}
              className={`notification-item ${notification.isRead ? 'read' : 'unread'} ${notification.type}`}
            >
              <div className="notification-icon">
                {notification.type === NotificationType.INFO && (
                  <span className="icon info">i</span>
                )}
                {notification.type === NotificationType.SUCCESS && (
                  <span className="icon success">✓</span>
                )}
                {notification.type === NotificationType.WARNING && (
                  <span className="icon warning">!</span>
                )}
                {notification.type === NotificationType.ERROR && (
                  <span className="icon error">×</span>
                )}
              </div>
              
              <div className="notification-content">
                <div className="notification-header">
                  <h3 className="notification-title">{notification.title}</h3>
                  <span className="notification-timestamp">{formatTimestamp(notification.timestamp)}</span>
                </div>
                <p className="notification-message">{notification.message}</p>
                {notification.relatedType && (
                  <div className="notification-related">
                    <span className="related-badge">{notification.relatedType}</span>
                  </div>
                )}
              </div>
              
              <div className="notification-actions">
                {!notification.isRead && (
                  <button 
                    className="action-button mark-read" 
                    onClick={() => handleMarkAsRead(notification.id)}
                  >
                    Mark as Read
                  </button>
                )}
                <button 
                  className="action-button delete" 
                  onClick={() => handleDelete(notification.id)}
                >
                  Delete
                </button>
              </div>
            </Card>
          ))
        )}
      </div>
    </div>
  );
};

export default NotificationsPanel; 