import React, { useCallback, useEffect, useState } from 'react';
import { Comment } from '../../types';
import { commentApi } from '../../api/comments';
import { useAuthStore } from '../../store/authStore';
import { websocketService } from '../../services/websocket';
import { Button } from '../common/Button';

interface CommentSectionProps {
  cardId: number;
}

export const CommentSection: React.FC<CommentSectionProps> = ({ cardId }) => {
  const [comments, setComments]     = useState<Comment[]>([]);
  const [newContent, setNewContent] = useState('');
  const [editId, setEditId]         = useState<number | null>(null);
  const [editContent, setEditContent] = useState('');
  const [loading, setLoading]       = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError]           = useState('');

  const { userId, username } = useAuthStore();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setComments(await commentApi.getByCardId(cardId));
    } catch {
      setError('Failed to load comments');
    } finally {
      setLoading(false);
    }
  }, [cardId]);

  useEffect(() => { load(); }, [load]);

  // Real-time updates via WebSocket
  useEffect(() => {
    if (!websocketService.isConnected()) return;

    websocketService.subscribeToCard(cardId, (message) => {
      if (
        message.type === 'COMMENT_ADDED' ||
        message.type === 'COMMENT_UPDATED' ||
        message.type === 'COMMENT_DELETED'
      ) {
        // Re-fetch the full thread so every update (including edits/deletes by
        // other users) is reflected without complex local state merging.
        load();
      }
    });

    return () => {
      websocketService.unsubscribeFromCard(cardId);
    };
  }, [cardId, load]);

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newContent.trim()) return;
    setSubmitting(true);
    setError('');
    try {
      const created = await commentApi.create(cardId, { content: newContent.trim() });
      setComments((prev) => [...prev, created]);
      setNewContent('');
    } catch {
      setError('Failed to post comment');
    } finally {
      setSubmitting(false);
    }
  };

  const handleEdit = async (commentId: number) => {
    if (!editContent.trim()) return;
    setSubmitting(true);
    setError('');
    try {
      const updated = await commentApi.update(commentId, { content: editContent.trim() });
      setComments((prev) => prev.map((c) => (c.id === commentId ? updated : c)));
      setEditId(null);
      setEditContent('');
    } catch {
      setError('Failed to update comment');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (commentId: number) => {
    if (!window.confirm('Delete this comment?')) return;
    setError('');
    try {
      await commentApi.delete(commentId);
      setComments((prev) => prev.filter((c) => c.id !== commentId));
    } catch {
      setError('Failed to delete comment');
    }
  };

  const startEdit = (comment: Comment) => {
    setEditId(comment.id);
    setEditContent(comment.content);
  };

  const formatDate = (iso: string) =>
    new Date(iso).toLocaleString(undefined, {
      dateStyle: 'medium',
      timeStyle: 'short',
    });

  return (
    <div className="mt-6 border-t pt-4">
      <h4 className="text-sm font-semibold text-gray-700 mb-3">
        Comments {comments.length > 0 && <span className="text-gray-400">({comments.length})</span>}
      </h4>

      {error && (
        <div className="bg-red-50 border border-red-400 text-red-700 px-3 py-2 rounded mb-3 text-sm">
          {error}
        </div>
      )}

      {/* Thread */}
      {loading ? (
        <p className="text-sm text-gray-400">Loading comments…</p>
      ) : comments.length === 0 ? (
        <p className="text-sm text-gray-400">No comments yet. Be the first!</p>
      ) : (
        <ul className="space-y-3 mb-4">
          {comments.map((c) => (
            <li key={c.id} className="flex gap-3">
              {/* Avatar */}
              <div className="flex-shrink-0 w-8 h-8 rounded-full bg-primary-600 flex items-center justify-center text-white text-xs font-semibold uppercase">
                {(c.authorUsername ?? '?').charAt(0)}
              </div>

              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="text-sm font-semibold text-gray-800">
                    {c.authorFullName || c.authorUsername || 'Deleted user'}
                  </span>
                  <span className="text-xs text-gray-400">{formatDate(c.createdAt)}</span>
                  {c.edited && (
                    <span className="text-xs text-gray-400 italic">(edited)</span>
                  )}
                </div>

                {editId === c.id ? (
                  <div className="mt-1">
                    <textarea
                      value={editContent}
                      onChange={(e) => setEditContent(e.target.value)}
                      rows={3}
                      maxLength={5000}
                      className="w-full px-2 py-1 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500"
                      autoFocus
                    />
                    <div className="flex gap-2 mt-1">
                      <Button
                        variant="primary"
                        onClick={() => handleEdit(c.id)}
                        loading={submitting}
                        className="text-xs py-1 px-3"
                      >
                        Save
                      </Button>
                      <Button
                        variant="ghost"
                        onClick={() => { setEditId(null); setEditContent(''); }}
                        className="text-xs py-1 px-3"
                      >
                        Cancel
                      </Button>
                    </div>
                  </div>
                ) : (
                  <>
                    <p className="text-sm text-gray-700 whitespace-pre-wrap break-words mt-0.5">
                      {c.content}
                    </p>
                    {c.authorId === userId && (
                      <div className="flex gap-2 mt-1">
                        <button
                          onClick={() => startEdit(c)}
                          className="text-xs text-primary-600 hover:underline"
                        >
                          Edit
                        </button>
                        <button
                          onClick={() => handleDelete(c.id)}
                          className="text-xs text-red-500 hover:underline"
                        >
                          Delete
                        </button>
                      </div>
                    )}
                  </>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}

      {/* New comment form */}
      <form onSubmit={handleAdd} className="flex gap-2 items-start">
        <div className="flex-shrink-0 w-8 h-8 rounded-full bg-primary-600 flex items-center justify-center text-white text-xs font-semibold uppercase">
          {(username ?? '?').charAt(0)}
        </div>
        <div className="flex-1">
          <textarea
            value={newContent}
            onChange={(e) => setNewContent(e.target.value)}
            placeholder="Write a comment…"
            rows={2}
            maxLength={5000}
            className="w-full px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"
          />
          <div className="flex justify-end mt-1">
            <Button
              type="submit"
              variant="primary"
              loading={submitting}
              className="text-xs py-1 px-3"
              disabled={!newContent.trim()}
            >
              Comment
            </Button>
          </div>
        </div>
      </form>
    </div>
  );
};

