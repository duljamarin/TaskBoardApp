import React from 'react';
import { Board } from '../../types';
import { Button } from '../common/Button';

interface BoardCardProps {
  board: Board;
  onClick: () => void;
  onDelete: () => void;
}

export const BoardCard: React.FC<BoardCardProps> = ({ board, onClick, onDelete }) => {
  const handleDelete = (e: React.MouseEvent) => {
    e.stopPropagation();
    onDelete();
  };

  const totalCards = board.lists.reduce((acc, list) => acc + list.cards.length, 0);

  return (
    <div
      className="relative bg-white rounded-2xl shadow-card hover:shadow-card-hover transition-all duration-300 cursor-pointer overflow-hidden group border border-surface-100 hover:border-surface-200"
      onClick={onClick}
    >
      {/* Color strip */}
      <div
        className="h-2"
        style={{ background: `linear-gradient(90deg, ${board.color || '#6366f1'}, ${board.color || '#6366f1'}88)` }}
      />
      <div className="p-5">
        <div className="flex items-start justify-between mb-3">
          <h3 className="text-base font-semibold text-surface-900 group-hover:text-primary-700 transition-colors line-clamp-1">
            {board.name}
          </h3>
          <div
            className="w-3 h-3 rounded-full flex-shrink-0 mt-1.5 ring-2 ring-white"
            style={{ backgroundColor: board.color || '#6366f1' }}
          />
        </div>

        {board.description && (
          <p className="text-sm text-surface-500 mb-4 line-clamp-2 leading-relaxed">{board.description}</p>
        )}

        <div className="flex items-center gap-4 text-xs text-surface-400 font-medium">
          <span className="flex items-center gap-1">
            <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h7" />
            </svg>
            {board.lists.length} lists
          </span>
          <span className="flex items-center gap-1">
            <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
            </svg>
            {totalCards} cards
          </span>
        </div>
      </div>

      {/* Delete button on hover */}
      <div className="absolute top-4 right-4 opacity-0 group-hover:opacity-100 transition-all duration-200 translate-y-1 group-hover:translate-y-0">
        <Button
          variant="danger"
          size="sm"
          onClick={handleDelete}
          className="shadow-md !rounded-lg !px-2 !py-1"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
        </Button>
      </div>
    </div>
  );
};

