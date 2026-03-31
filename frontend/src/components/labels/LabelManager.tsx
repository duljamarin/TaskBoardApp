import React, { useEffect, useState } from 'react';
import { Label } from '../../types';
import { labelApi } from '../../api';
import { Modal } from '@/components';
import { Input } from '@/components';
import { Button } from '@/components';
import { LabelBadge } from './LabelBadge';

interface LabelManagerProps {
  isOpen: boolean;
  onClose: () => void;
  boardId: number;
}

const PRESET_COLORS = [
  '#e74c3c', '#e67e22', '#f1c40f', '#2ecc71', '#1abc9c',
  '#3498db', '#9b59b6', '#34495e', '#95a5a6', '#d35400',
];

/**
 * Modal for managing (create/edit/delete) labels on a board.
 */
export const LabelManager: React.FC<LabelManagerProps> = ({ isOpen, onClose, boardId }) => {
  const [labels, setLabels] = useState<Label[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Create form state
  const [newName, setNewName] = useState('');
  const [newColor, setNewColor] = useState(PRESET_COLORS[0]);

  // Edit state
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editName, setEditName] = useState('');
  const [editColor, setEditColor] = useState('');

  useEffect(() => {
    if (isOpen) {
      fetchLabels();
    }
  }, [isOpen, boardId]);

  const fetchLabels = async () => {
    try {
      const data = await labelApi.getByBoardId(boardId);
      setLabels(data);
    } catch {
      setError('Failed to load labels');
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newName.trim()) return;
    setLoading(true);
    setError('');
    try {
      const label = await labelApi.create(boardId, { name: newName.trim(), color: newColor });
      setLabels((prev) => [...prev, label].sort((a, b) => a.name.localeCompare(b.name)));
      setNewName('');
      setNewColor(PRESET_COLORS[0]);
    } catch (err: any) {
      setError(err?.response?.data?.message || err?.message || 'Failed to create label');
    } finally {
      setLoading(false);
    }
  };

  const startEdit = (label: Label) => {
    setEditingId(label.id);
    setEditName(label.name);
    setEditColor(label.color);
  };

  const handleUpdate = async () => {
    if (!editingId || !editName.trim()) return;
    setLoading(true);
    setError('');
    try {
      const updated = await labelApi.update(editingId, { name: editName.trim(), color: editColor });
      setLabels((prev) =>
        prev.map((l) => (l.id === editingId ? updated : l)).sort((a, b) => a.name.localeCompare(b.name))
      );
      setEditingId(null);
    } catch (err: any) {
      setError(err?.response?.data?.message || err?.message || 'Failed to update label');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Delete this label? It will be removed from all cards.')) return;
    setLoading(true);
    try {
      await labelApi.delete(id);
      setLabels((prev) => prev.filter((l) => l.id !== id));
    } catch (err: any) {
      setError(err?.message || 'Failed to delete label');
    } finally {
      setLoading(false);
    }
  };

  const ColorPicker = ({ selected, onSelect }: { selected: string; onSelect: (c: string) => void }) => (
    <div className="flex flex-wrap gap-1.5">
      {PRESET_COLORS.map((color) => (
        <button
          key={color}
          type="button"
          onClick={() => onSelect(color)}
          className={`w-6 h-6 rounded-full border-2 transition-transform ${
            selected === color ? 'border-gray-800 scale-110' : 'border-transparent'
          }`}
          style={{ backgroundColor: color }}
        />
      ))}
    </div>
  );

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Manage Labels" size="md">
      {error && (
        <div className="bg-red-50 border border-red-400 text-red-700 px-3 py-2 rounded mb-3 text-sm">
          {error}
        </div>
      )}

      {/* Create new label */}
      <form onSubmit={handleCreate} className="mb-4 p-3 bg-gray-50 rounded-lg space-y-3">
        <p className="text-sm font-semibold text-gray-700">Create Label</p>
        <Input
          label="Name"
          type="text"
          value={newName}
          onChange={(e) => setNewName(e.target.value)}
          placeholder="e.g. Bug, Feature, Urgent"
          required
        />
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Color</label>
          <ColorPicker selected={newColor} onSelect={setNewColor} />
        </div>
        <div className="flex items-center gap-2">
          <div className="flex-1">
            {newName && <LabelBadge label={{ id: 0, name: newName, color: newColor, boardId, createdAt: '', updatedAt: '' }} size="md" />}
          </div>
          <Button type="submit" variant="primary" size="sm" loading={loading}>
            Create
          </Button>
        </div>
      </form>

      {/* Existing labels list */}
      <div className="space-y-2 max-h-64 overflow-y-auto">
        {labels.length === 0 && (
          <p className="text-sm text-gray-500 text-center py-4">No labels yet. Create one above!</p>
        )}

        {labels.map((label) =>
          editingId === label.id ? (
            <div key={label.id} className="p-2 border border-primary-200 rounded-lg bg-primary-50 space-y-2">
              <Input
                label="Name"
                type="text"
                value={editName}
                onChange={(e) => setEditName(e.target.value)}
              />
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Color</label>
                <ColorPicker selected={editColor} onSelect={setEditColor} />
              </div>
              <div className="flex justify-end gap-2">
                <Button variant="ghost" size="sm" onClick={() => setEditingId(null)}>
                  Cancel
                </Button>
                <Button variant="primary" size="sm" onClick={handleUpdate} loading={loading}>
                  Save
                </Button>
              </div>
            </div>
          ) : (
            <div key={label.id} className="flex items-center justify-between p-2 hover:bg-gray-50 rounded-lg">
              <LabelBadge label={label} size="md" />
              <div className="flex items-center gap-1">
                <button
                  onClick={() => startEdit(label)}
                  className="p-1 text-gray-400 hover:text-gray-600 rounded"
                  title="Edit"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                      d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                  </svg>
                </button>
                <button
                  onClick={() => handleDelete(label.id)}
                  className="p-1 text-gray-400 hover:text-red-600 rounded"
                  title="Delete"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                      d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>
            </div>
          )
        )}
      </div>
    </Modal>
  );
};

