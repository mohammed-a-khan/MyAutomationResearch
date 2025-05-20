/**
 * Settings Service - Handles API calls for application settings
 */
import api from './api';

// Define types for settings
export interface GeneralSettings {
  defaultBrowser: string;
  defaultTimeout: number;
  maxRetries: number;
  screenshotOnError: boolean;
  logsEnabled: boolean;
  defaultEnvironment: string;
}

export interface ParallelExecutionSettings {
  enabled: boolean;
  maxParallelExecutions: number;
}

export interface NotificationSettings {
  emailNotifications: boolean;
  emailRecipients: string;
  slackIntegration: boolean;
  slackWebhook: string;
  notifyOnSuccess: boolean;
  notifyOnFailure: boolean;
}

/**
 * Get general settings
 * @returns Promise with general settings
 */
export const getGeneralSettings = async (): Promise<GeneralSettings> => {
  try {
    return await api.get<GeneralSettings>('/settings/general');
  } catch (error) {
    console.error('Failed to fetch general settings:', error);
    throw error;
  }
};

/**
 * Update general settings
 * @param settings General settings to update
 * @returns Promise that resolves when settings are updated
 */
export const updateGeneralSettings = async (settings: GeneralSettings): Promise<void> => {
  try {
    await api.put('/settings/general', settings);
  } catch (error) {
    console.error('Failed to update general settings:', error);
    throw error;
  }
};

/**
 * Get parallel execution settings
 * @returns Promise with parallel execution settings
 */
export const getParallelSettings = async (): Promise<ParallelExecutionSettings> => {
  try {
    return await api.get<ParallelExecutionSettings>('/settings/parallel');
  } catch (error) {
    console.error('Failed to fetch parallel execution settings:', error);
    throw error;
  }
};

/**
 * Update parallel execution settings
 * @param settings Parallel execution settings to update
 * @returns Promise that resolves when settings are updated
 */
export const updateParallelSettings = async (settings: ParallelExecutionSettings): Promise<void> => {
  try {
    await api.put('/settings/parallel', settings);
  } catch (error) {
    console.error('Failed to update parallel execution settings:', error);
    throw error;
  }
};

/**
 * Get notification settings
 * @returns Promise with notification settings
 */
export const getNotificationSettings = async (): Promise<NotificationSettings> => {
  try {
    return await api.get<NotificationSettings>('/settings/notifications');
  } catch (error) {
    console.error('Failed to fetch notification settings:', error);
    throw error;
  }
};

/**
 * Update notification settings
 * @param settings Notification settings to update
 * @returns Promise that resolves when settings are updated
 */
export const updateNotificationSettings = async (settings: NotificationSettings): Promise<void> => {
  try {
    await api.put('/settings/notifications', settings);
  } catch (error) {
    console.error('Failed to update notification settings:', error);
    throw error;
  }
};

// Export as default object for easy import
export default {
  getGeneralSettings,
  updateGeneralSettings,
  getParallelSettings,
  updateParallelSettings,
  getNotificationSettings,
  updateNotificationSettings
}; 