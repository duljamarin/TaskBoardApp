// Auth types
export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  fullName?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  userId: number;
  username: string;
  email: string;
  roles: string[];
}

// Admin types
export interface UserDTO {
  id: number;
  username: string;
  email: string;
  fullName?: string;
  active: boolean;
  roles: string[];
  createdAt: string;
  updatedAt: string;
}

export interface RoleAssignmentRequest {
  roleName: string;
}

// Board types
export interface Board {
  id: number;
  name: string;
  description?: string;
  color: string;
  ownerId: number;
  ownerUsername: string;
  archived: boolean;
  lists: List[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateBoardRequest {
  name: string;
  description?: string;
  color?: string;
}

// List types
export interface List {
  id: number;
  name: string;
  boardId: number;
  position: number;
  cards: Card[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateListRequest {
  name: string;
  boardId: number;
  position?: number;
}

// Card types
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

// Label types
export interface Label {
  id: number;
  name: string;
  color: string;
  boardId: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateLabelRequest {
  name: string;
  color?: string;
}

export interface Card {
  id: number;
  title: string;
  description?: string;
  listId: number;
  listName: string;
  position: number;
  assignedToId?: number;
  assignedToUsername?: string;
  assignedToFullName?: string;
  priority: Priority;
  dueDate?: string;
  labels: Label[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateCardRequest {
  title: string;
  description?: string;
  listId: number;
  position?: number;
  priority?: Priority;
  dueDate?: string;
  assignedToId?: number;
}

export interface CardMoveRequest {
  newListId: number;
  newPosition: number;
}

// User types
export interface User {
  id: number;
  username: string;
  email: string;
  fullName?: string;
}

// Comment types
export interface Comment {
  id: number;
  cardId: number;
  authorId: number | null;
  authorUsername: string | null;
  authorFullName: string | null;
  content: string;
  edited: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCommentRequest {
  content: string;
}

