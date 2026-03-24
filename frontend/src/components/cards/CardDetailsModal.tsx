import React, { useState } from 'react';
import { Card, Priority } from '../../types';
import { useBoardStore } from '../../store/boardStore';
import { Modal } from '../common/Modal';
import { Input } from '../common/Input';
import { Button } from '../common/Button';
import { CommentSection } from './CommentSection';

interface CardDetailsModalProps {
  isOpen: boolean;
  onClose: () => void;
  card: Card;
}

export const CardDetailsModal: React.FC<CardDetailsModalProps> = ({ isOpen, onClose, card }) => {
  const [isEditing, setIsEditing] = useState(false);
  const [title, setTitle] = useState(card.title);
  const [description, setDescription] = useState(card.description || '');
  const [priority, setPriority] = useState<Priority>(card.priority);
  const [dueDate, setDueDate] = useState(
    card.dueDate ? new Date(card.dueDate).toISOString().split('T')[0] : ''
  );
  const [error, setError] = useState('');

  const { updateCard, deleteCard, loading } = useBoardStore();

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!title.trim()) {
      setError('Card title is required');
      return;
    }

    try {
      await updateCard(card.id, {
        title: title.trim(),
        description: description.trim() || undefined,
        listId: card.listId,
        priority,
        dueDate: dueDate || undefined,
      });
      setIsEditing(false);
    } catch (err: any) {
      setError(err.message || 'Failed to update card');
    }
  };

  const handleDelete = async () => {
    if (window.confirm('Are you sure you want to delete this card?')) {
      try {
        await deleteCard(card.id);
        onClose();
      } catch (err: any) {
        setError(err.message || 'Failed to delete card');
      }
    }
  };

  const handleClose = () => {
    setIsEditing(false);
    setTitle(card.title);
    setDescription(card.description || '');
    setPriority(card.priority);
    setDueDate(card.dueDate ? new Date(card.dueDate).toISOString().split('T')[0] : '');
    setError('');
    onClose();
  };

  const isOverdue = card.dueDate && new Date(card.dueDate) < new Date();

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title={isEditing ? 'Edit Card' : 'Card Details'}>
      {error && (
        <div className="bg-red-50 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
          {error}
        </div>
      )}

      {isEditing ? (
        <form onSubmit={handleUpdate} className="space-y-4">
          <Input
            label="Card Title"
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
          />

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Description
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={4}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Priority
            </label>
            <select
              value={priority}
              onChange={(e) => setPriority(e.target.value as Priority)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500"
            >
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="CRITICAL">Critical</option>
            </select>
          </div>

          <Input
            label="Due Date"
            type="date"
            value={dueDate}
            onChange={(e) => setDueDate(e.target.value)}
          />

          <div className="flex justify-end space-x-2 pt-4">
            <Button type="button" variant="ghost" onClick={() => setIsEditing(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" loading={loading}>
              Save Changes
            </Button>
          </div>
        </form>
      ) : (
        <div className="space-y-4">
          <div>
            <h3 className="text-xl font-semibold text-gray-900 mb-2">{card.title}</h3>
            {card.description && (
              <p className="text-gray-700 whitespace-pre-wrap">{card.description}</p>
            )}
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <span className="text-sm font-medium text-gray-500">Priority:</span>
              <span className={`ml-2 text-sm px-2 py-1 rounded ${
                card.priority === 'LOW' ? 'bg-gray-100 text-gray-800' :
                card.priority === 'MEDIUM' ? 'bg-blue-100 text-blue-800' :
                card.priority === 'HIGH' ? 'bg-orange-100 text-orange-800' :
                'bg-red-100 text-red-800'
              }`}>
                {card.priority}
              </span>
            </div>

            {card.dueDate && (
              <div>
                <span className="text-sm font-medium text-gray-500">Due Date:</span>
                <span className={`ml-2 text-sm ${isOverdue ? 'text-red-600 font-semibold' : 'text-gray-700'}`}>
                  {new Date(card.dueDate).toLocaleDateString()}
                  {isOverdue && ' (Overdue)'}
                </span>
              </div>
            )}

            <div>
              <span className="text-sm font-medium text-gray-500">List:</span>
              <span className="ml-2 text-sm text-gray-700">{card.listName}</span>
            </div>

            {card.assignedToUsername && (
              <div>
                <span className="text-sm font-medium text-gray-500">Assigned to:</span>
                <span className="ml-2 text-sm text-gray-700">
                  {card.assignedToFullName || `@${card.assignedToUsername}`}
                </span>
              </div>
            )}
          </div>

          <div className="border-t pt-4">
            <div className="text-xs text-gray-500">
              <div>Created: {new Date(card.createdAt).toLocaleString()}</div>
              <div>Updated: {new Date(card.updatedAt).toLocaleString()}</div>
            </div>
          </div>

          {/* Comments thread */}
          <CommentSection cardId={card.id} />

          <div className="flex justify-between pt-4">
            <Button variant="danger" onClick={handleDelete} loading={loading}>
              Delete Card
            </Button>
            <div className="flex space-x-2">
              <Button variant="ghost" onClick={handleClose}>
                Close
              </Button>
              <Button variant="primary" onClick={() => setIsEditing(true)}>
                Edit Card
              </Button>
            </div>
          </div>
        </div>
      )}
    </Modal>
  );
};

