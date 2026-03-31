import React, { useEffect, useState } from 'react';
import { Label } from '../../types';
import { labelApi } from '../../api';
import { LabelBadge } from './LabelBadge';

interface LabelPickerProps {
  cardId: number;
  boardId: number;
  /** Labels currently assigned to the card */
  assignedLabels: Label[];
  /** Called after a label is toggled so the parent can refresh */
  onChanged: () => void;
}

/**
 * Dropdown-style component that lets users toggle labels on a card.
 */
export const LabelPicker: React.FC<LabelPickerProps> = ({
  cardId,
  boardId,
  assignedLabels,
  onChanged,
}) => {
  const [boardLabels, setBoardLabels] = useState<Label[]>([]);
  const [loading, setLoading] = useState(false);
  const [isOpen, setIsOpen] = useState(false);

  useEffect(() => {
    if (isOpen) {
      fetchBoardLabels();
    }
  }, [isOpen, boardId]);

  const fetchBoardLabels = async () => {
    try {
      const labels = await labelApi.getByBoardId(boardId);
      setBoardLabels(labels);
    } catch (err) {
      console.error('Failed to fetch board labels', err);
    }
  };

  const isAssigned = (labelId: number): boolean =>
    assignedLabels.some((l) => l.id === labelId);

  const toggleLabel = async (label: Label) => {
    setLoading(true);
    try {
      if (isAssigned(label.id)) {
        await labelApi.removeFromCard(cardId, label.id);
      } else {
        await labelApi.assignToCard(cardId, label.id);
      }
      onChanged();
    } catch (err) {
      console.error('Failed to toggle label', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="text-sm text-primary-600 hover:text-primary-800 font-medium flex items-center gap-1"
      >
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
            d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A2 2 0 013 12V7a4 4 0 014-4z" />
        </svg>
        Labels
      </button>

      {isOpen && (
        <div className="absolute z-50 mt-1 w-56 bg-white border border-gray-200 rounded-lg shadow-lg">
          <div className="p-2">
            <p className="text-xs font-semibold text-gray-500 uppercase mb-2">Board Labels</p>

            {boardLabels.length === 0 && (
              <p className="text-xs text-gray-400 py-2">No labels yet. Create them from the board view.</p>
            )}

            <div className="space-y-1 max-h-48 overflow-y-auto">
              {boardLabels.map((label) => (
                <button
                  key={label.id}
                  onClick={() => toggleLabel(label)}
                  disabled={loading}
                  className={`w-full flex items-center justify-between px-2 py-1.5 rounded text-left hover:bg-gray-50 transition-colors ${
                    loading ? 'opacity-50' : ''
                  }`}
                >
                  <LabelBadge label={label} size="md" />
                  {isAssigned(label.id) && (
                    <svg className="w-4 h-4 text-green-600 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd"
                        d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                        clipRule="evenodd" />
                    </svg>
                  )}
                </button>
              ))}
            </div>
          </div>

          <div className="border-t px-2 py-2">
            <button
              onClick={() => setIsOpen(false)}
              className="w-full text-xs text-gray-500 hover:text-gray-700 text-center"
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

