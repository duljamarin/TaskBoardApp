import api from './axios';
import { Label, CreateLabelRequest } from '../types';

export const labelApi = {
  // Board-level CRUD
  getByBoardId: async (boardId: number): Promise<Label[]> => {
    const response = await api.get<Label[]>(`/boards/${boardId}/labels`);
    return response.data;
  },

  create: async (boardId: number, data: CreateLabelRequest): Promise<Label> => {
    const response = await api.post<Label>(`/boards/${boardId}/labels`, data);
    return response.data;
  },

  update: async (id: number, data: CreateLabelRequest): Promise<Label> => {
    const response = await api.put<Label>(`/labels/${id}`, data);
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/labels/${id}`);
  },

  // Card-level assignment
  assignToCard: async (cardId: number, labelId: number): Promise<void> => {
    await api.post(`/cards/${cardId}/labels/${labelId}`);
  },

  removeFromCard: async (cardId: number, labelId: number): Promise<void> => {
    await api.delete(`/cards/${cardId}/labels/${labelId}`);
  },

  getByCardId: async (cardId: number): Promise<Label[]> => {
    const response = await api.get<Label[]>(`/cards/${cardId}/labels`);
    return response.data;
  },
};

