import api from './axios';
import { UserSummary } from '@/types';

export const userApi = {
  listUsers: async (): Promise<UserSummary[]> => {
    const response = await api.get<UserSummary[]>('/users');
    return response.data;
  },
};
