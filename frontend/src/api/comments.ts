import api from './axios';
import { Comment, CreateCommentRequest } from '../types';

export const commentApi = {
  /** Fetch all comments for a card (GET /api/v1/cards/{cardId}/comments) */
  getByCardId: async (cardId: number): Promise<Comment[]> => {
    const response = await api.get<Comment[]>(`/cards/${cardId}/comments`);
    return response.data;
  },

  /** Post a new comment (POST /api/v1/cards/{cardId}/comments) */
  create: async (cardId: number, data: CreateCommentRequest): Promise<Comment> => {
    const response = await api.post<Comment>(`/cards/${cardId}/comments`, data);
    return response.data;
  },

  /** Edit an existing comment (PUT /api/v1/comments/{id}) */
  update: async (commentId: number, data: CreateCommentRequest): Promise<Comment> => {
    const response = await api.put<Comment>(`/comments/${commentId}`, data);
    return response.data;
  },

  /** Delete a comment (DELETE /api/v1/comments/{id}) */
  delete: async (commentId: number): Promise<void> => {
    await api.delete(`/comments/${commentId}`);
  },
};

