import React, { useState } from 'react';
import { useBoardStore } from '../../store/boardStore';
import { Modal } from '../common/Modal';
import { Input } from '../common/Input';
import { Button } from '../common/Button';

interface CreateBoardModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const COLORS = [
  '#3B82F6', // Blue
  '#10B981', // Green
  '#F59E0B', // Amber
  '#EF4444', // Red
  '#8B5CF6', // Purple
  '#EC4899', // Pink
  '#06B6D4', // Cyan
  '#84CC16', // Lime
];

export const CreateBoardModal: React.FC<CreateBoardModalProps> = ({ isOpen, onClose }) => {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [color, setColor] = useState(COLORS[0]);
  const [error, setError] = useState('');

  const { createBoard, loading } = useBoardStore();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!name.trim()) {
      setError('Board name is required');
      return;
    }

    try {
      await createBoard({
        name: name.trim(),
        description: description.trim() || undefined,
        color,
      });
      setName('');
      setDescription('');
      setColor(COLORS[0]);
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to create board');
    }
  };

  const handleClose = () => {
    setName('');
    setDescription('');
    setColor(COLORS[0]);
    setError('');
    onClose();
  };

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title="Create New Board">
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-xl text-sm">
            {error}
          </div>
        )}

        <Input
          label="Board Name"
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Enter board name"
          required
        />

        <div>
          <label className="block text-sm font-medium text-surface-700 mb-1.5">
            Description (Optional)
          </label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Enter board description"
            rows={3}
            className="w-full px-4 py-2.5 bg-white border border-surface-200 rounded-xl shadow-sm text-sm text-surface-900 placeholder-surface-400 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 hover:border-surface-400"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-surface-700 mb-1.5">
            Board Color
          </label>
          <div className="grid grid-cols-8 gap-2">
            {COLORS.map((c) => (
              <button
                key={c}
                type="button"
                onClick={() => setColor(c)}
                className={`w-10 h-10 rounded-xl transition-all duration-200 ${
                  color === c ? 'ring-2 ring-offset-2 ring-primary-400 scale-110' : 'hover:scale-105'
                }`}
                style={{ backgroundColor: c }}
              />
            ))}
          </div>
        </div>

        <div className="flex justify-end gap-2 pt-4 border-t border-surface-100">
          <Button type="button" variant="ghost" onClick={handleClose}>
            Cancel
          </Button>
          <Button type="submit" variant="gradient" loading={loading}>
            Create Board
          </Button>
        </div>
      </form>
    </Modal>
  );
};
