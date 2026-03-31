import api from './axios';
import { UserDTO, RoleAssignmentRequest } from '../types';

export const adminApi = {
  getAllUsers: async (): Promise<UserDTO[]> => {
    const response = await api.get<UserDTO[]>('/admin/users');
    return response.data;
  },

  getUserById: async (userId: number): Promise<UserDTO> => {
    const response = await api.get<UserDTO>(`/admin/users/${userId}`);
    return response.data;
  },

  assignRole: async (userId: number, data: RoleAssignmentRequest): Promise<UserDTO> => {
    const response = await api.post<UserDTO>(`/admin/users/${userId}/roles`, data);
    return response.data;
  },

  removeRole: async (userId: number, roleName: string): Promise<UserDTO> => {
    const response = await api.delete<UserDTO>(`/admin/users/${userId}/roles/${roleName}`);
    return response.data;
  },

  toggleUserStatus: async (userId: number): Promise<UserDTO> => {
    const response = await api.patch<UserDTO>(`/admin/users/${userId}/status`);
    return response.data;
  },

  getAllRoles: async (): Promise<{ id: number; name: string }[]> => {
    const response = await api.get<{ id: number; name: string }[]>('/admin/roles');
    return response.data;
  },
};

