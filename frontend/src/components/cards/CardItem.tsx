import React, { useState } from 'react';
import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { Card } from '@/types';
import { CardDetailsModal } from '@/components';
import { LabelBadge } from '@/components';

interface CardItemProps {
  card: Card;
}

const PRIORITY_COLORS = {
  LOW: 'bg-gray-100 text-gray-800',
  MEDIUM: 'bg-blue-100 text-blue-800',
  HIGH: 'bg-orange-100 text-orange-800',
  CRITICAL: 'bg-red-100 text-red-800',
};

export const CardItem: React.FC<CardItemProps> = ({ card }) => {
  const [isModalOpen, setIsModalOpen] = useState(false);

  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({
    id: card.id,
    data: {
      type: 'card',
      card,
    },
  });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  };

  const getDueDateStatus = () => {
    if (!card.dueDate) return null;
    const due = new Date(card.dueDate);
    const now = new Date();
    if (due < now) return 'overdue';
    const threeDaysFromNow = new Date(now.getTime() + 3 * 24 * 60 * 60 * 1000);
    if (due <= threeDaysFromNow) return 'soon';
    return 'normal';
  };
  const dueDateStatus = getDueDateStatus();

  return (
    <>
      <div
        ref={setNodeRef}
        style={style}
        {...attributes}
        {...listeners}
        onClick={() => setIsModalOpen(true)}
        className="bg-white rounded-lg p-3 shadow-sm hover:shadow-md transition-shadow cursor-grab active:cursor-grabbing"
      >
        {/* Label badges */}
        {card.labels && card.labels.length > 0 && (
          <div className="flex flex-wrap gap-1 mb-2">
            {card.labels.map((label) => (
              <LabelBadge key={label.id} label={label} size="sm" />
            ))}
          </div>
        )}

        <h4 className="text-sm font-medium text-gray-900 mb-2">{card.title}</h4>

        {card.description && (
          <p className="text-xs text-gray-600 mb-2 line-clamp-2">{card.description}</p>
        )}

        <div className="flex flex-wrap gap-2 items-center">
          <span className={`text-xs px-2 py-1 rounded ${PRIORITY_COLORS[card.priority]}`}>
            {card.priority}
          </span>

          {card.dueDate && (
            <span className={`text-xs px-2 py-1 rounded ${
              dueDateStatus === 'overdue' ? 'bg-red-100 text-red-800' :
              dueDateStatus === 'soon' ? 'bg-amber-100 text-amber-800' :
              'bg-gray-100 text-gray-800'
            }`}>
              {new Date(card.dueDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
            </span>
          )}

          {card.assignedToUsername && (
            <span className="text-xs px-2 py-1 rounded bg-purple-100 text-purple-800">
              @{card.assignedToUsername}
            </span>
          )}
        </div>
      </div>

      <CardDetailsModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        card={card}
      />
    </>
  );
};

