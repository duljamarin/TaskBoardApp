import api from './axios';
import { Notification } from '@/types';

export const notificationApi = {
  getNotifications: async (page = 0, size = 20): Promise<{ notifications: Notification[]; totalPages: number; totalElements: number }> => {
    const response = await api.get<{ content: Notification[]; totalPages: number; totalElements: number }>('/notifications', {
      params: { page, size },
    });
    return {
      notifications: response.data.content,
      totalPages: response.data.totalPages,
      totalElements: response.data.totalElements,
    };
  },

  getUnreadNotifications: async (): Promise<Notification[]> => {
    const response = await api.get<Notification[]>('/notifications/unread');
    return response.data;
  },

  getUnreadCount: async (): Promise<number> => {
    const response = await api.get<{ count: number }>('/notifications/unread/count');
    return response.data.count;
  },

  markAsRead: async (id: number): Promise<Notification> => {
    const response = await api.patch<Notification>(`/notifications/${id}/read`);
    return response.data;
  },

  markAllAsRead: async (): Promise<number> => {
    const response = await api.patch<{ updated: number }>('/notifications/read-all');
    return response.data.updated;
  },
};
