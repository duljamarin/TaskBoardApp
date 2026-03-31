import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useBoardStore } from '../../store/boardStore';
import { Button } from '../common/Button';
import { BoardCard } from './BoardCard';
import { CreateBoardModal } from './CreateBoardModal';

export const BoardList: React.FC = () => {
  const { boards, loading, error, fetchBoards, deleteBoard } = useBoardStore();
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    fetchBoards();
  }, [fetchBoards]);

  const handleBoardClick = (boardId: number) => {
    navigate(`/boards/${boardId}`);
  };

  const handleDeleteBoard = async (boardId: number) => {
    if (window.confirm('Are you sure you want to delete this board?')) {
      try {
        await deleteBoard(boardId);
      } catch (error) {
        console.error('Failed to delete board:', error);
      }
    }
  };

  if (loading && boards.length === 0) {
    return (
      <div className="flex justify-center items-center min-h-[60vh]">
        <div className="flex flex-col items-center gap-3">
          <div className="w-10 h-10 border-3 border-primary-200 border-t-primary-600 rounded-full animate-spin" />
          <span className="text-sm text-surface-500 font-medium">Loading boards…</span>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4 mb-8">
        <div>
          <h1 className="text-2xl font-bold text-surface-900">My Boards</h1>
          <p className="text-surface-500 text-sm mt-1">
            {boards.length} board{boards.length !== 1 ? 's' : ''} • Organize your projects
          </p>
        </div>
        <Button variant="gradient" onClick={() => setIsCreateModalOpen(true)}>
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          New Board
        </Button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-xl mb-6 text-sm">
          {error}
        </div>
      )}

      {boards.length === 0 ? (
        <div className="text-center py-20">
          <div className="w-20 h-20 mx-auto mb-6 rounded-2xl bg-primary-50 flex items-center justify-center">
            <svg className="w-10 h-10 text-primary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
            </svg>
          </div>
          <h2 className="text-xl font-semibold text-surface-900 mb-2">No boards yet</h2>
          <p className="text-surface-500 mb-6 max-w-sm mx-auto">
            Create your first board to start organizing tasks and collaborating with your team.
          </p>
          <Button variant="gradient" onClick={() => setIsCreateModalOpen(true)}>
            Create Your First Board
          </Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
          {boards.map((board) => (
            <BoardCard
              key={board.id}
              board={board}
              onClick={() => handleBoardClick(board.id)}
              onDelete={() => handleDeleteBoard(board.id)}
            />
          ))}
        </div>
      )}

      <CreateBoardModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
      />
    </div>
  );
};

