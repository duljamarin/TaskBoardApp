import React from 'react';
import { Label } from '../../types';

interface LabelBadgeProps {
  label: Label;
  size?: 'sm' | 'md';
  onRemove?: () => void;
}

/**
 * Colored pill that displays a label on a card.
 */
export const LabelBadge: React.FC<LabelBadgeProps> = ({ label, size = 'sm', onRemove }) => {
  // Determine if label color is dark to choose text color
  const isDark = (color: string): boolean => {
    const hex = color.replace('#', '');
    const r = parseInt(hex.substring(0, 2), 16);
    const g = parseInt(hex.substring(2, 4), 16);
    const b = parseInt(hex.substring(4, 6), 16);
    const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
    return luminance < 0.5;
  };

  const textColor = isDark(label.color) ? 'white' : '#1a202c';

  return (
    <span
      className={`inline-flex items-center rounded-full font-medium ${
        size === 'sm' ? 'text-xs px-2 py-0.5' : 'text-sm px-2.5 py-0.5'
      }`}
      style={{ backgroundColor: label.color, color: textColor }}
    >
      {label.name}
      {onRemove && (
        <button
          onClick={(e) => {
            e.stopPropagation();
            onRemove();
          }}
          className="ml-1 hover:opacity-75 focus:outline-none"
          style={{ color: textColor }}
        >
          ×
        </button>
      )}
    </span>
  );
};

