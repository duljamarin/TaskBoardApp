import api from './axios';
import { Board, BoardMember, CreateBoardRequest } from '../types';

export const boardApi = {
  getAll: async (): Promise<Board[]> => {
    const response = await api.get<Board[]>('/boards');
    return response.data;
  },

  getById: async (id: number): Promise<Board> => {
    const response = await api.get<Board>(`/boards/${id}`);
    return response.data;
  },

  create: async (data: CreateBoardRequest): Promise<Board> => {
    const response = await api.post<Board>('/boards', data);
    return response.data;
  },

  update: async (id: number, data: CreateBoardRequest): Promise<Board> => {
    const response = await api.put<Board>(`/boards/${id}`, data);
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/boards/${id}`);
  },

  getMembers: async (boardId: number): Promise<BoardMember[]> => {
    const response = await api.get<BoardMember[]>(`/boards/${boardId}/members`);
    return response.data;
  },

  addMember: async (boardId: number, userId: number, role: string = 'MEMBER'): Promise<BoardMember> => {
    const response = await api.post<BoardMember>(`/boards/${boardId}/members/${userId}?role=${role}`);
    return response.data;
  },

  removeMember: async (boardId: number, userId: number): Promise<void> => {
    await api.delete(`/boards/${boardId}/members/${userId}`);
  },
};

