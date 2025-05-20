/**
 * Notification Service - Handles API calls for notifications
 */
import api from './api';
import { buildApiUrlWithParams } from '../utils/apiClient';
import { NotificationType } from '../components/notifications/NotificationsPanel';

// Notification interface
export interface Notification {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  timestamp: string;
  isRead: boolean;
  relatedId?: string;
  relatedType?: 'execution' | 'project' | 'testcase' | 'system';
}

/**
 * Get all notifications
 * @returns Promise with list of notifications
 */
export const getNotifications = async (): Promise<Notification[]> => {
  try {
    return await api.get<Notification[]>('/notifications');
  } catch (error) {
    console.error('Failed to fetch notifications:', error);
    throw error;
  }
};

/**
 * Mark a notification as read
 * @param id Notification ID
 * @returns Promise that resolves when the notification is marked as read
 */
export const markAsRead = async (id: string): Promise<void> => {
  try {
    await api.put(`/notifications/${id}/read`);
  } catch (error) {
    console.error(`Failed to mark notification as read (id: ${id}):`, error);
    throw error;
  }
};

/**
 * Mark all notifications as read
 * @returns Promise that resolves when all notifications are marked as read
 */
export const markAllAsRead = async (): Promise<void> => {
  try {
    await api.put('/notifications/read-all');
  } catch (error) {
    console.error('Failed to mark all notifications as read:', error);
    throw error;
  }
};

/**
 * Delete a notification
 * @param id Notification ID
 * @returns Promise that resolves when the notification is deleted
 */
export const deleteNotification = async (id: string): Promise<void> => {
  try {
    await api.delete(`/notifications/${id}`);
  } catch (error) {
    console.error(`Failed to delete notification (id: ${id}):`, error);
    throw error;
  }
};

/**
 * Delete all notifications
 * @returns Promise that resolves when all notifications are deleted
 */
export const deleteAllNotifications = async (): Promise<void> => {
  try {
    await api.delete('/notifications');
  } catch (error) {
    console.error('Failed to delete all notifications:', error);
    throw error;
  }
};

// Export as default object for easy import
export default {
  getNotifications,
  markAsRead,
  markAllAsRead,
  deleteNotification,
  deleteAllNotifications
}; 