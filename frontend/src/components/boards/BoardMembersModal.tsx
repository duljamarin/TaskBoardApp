import React, { useEffect, useState } from 'react';
import { BoardMember, UserSummary } from '../../types';
import { boardApi, userApi } from '../../api';
import { Modal } from '../common/Modal';
import { Button } from '../common/Button';
import { useAuthStore } from '../../store/authStore';

interface BoardMembersModalProps {
  isOpen: boolean;
  onClose: () => void;
  boardId: number;
  ownerId: number;
}

const ROLE_LABELS: Record<string, { label: string; color: string }> = {
  OWNER: { label: 'Owner', color: 'bg-amber-100 text-amber-800' },
  EDITOR: { label: 'Editor', color: 'bg-blue-100 text-blue-800' },
  MEMBER: { label: 'Member', color: 'bg-gray-100 text-gray-700' },
};

export const BoardMembersModal: React.FC<BoardMembersModalProps> = ({
  isOpen,
  onClose,
  boardId,
  ownerId,
}) => {
  const { userId: currentUserId } = useAuthStore();
  const [members, setMembers] = useState<BoardMember[]>([]);
  const [allUsers, setAllUsers] = useState<UserSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedRole, setSelectedRole] = useState('MEMBER');

  const isOwner = currentUserId === ownerId;

  useEffect(() => {
    if (isOpen) {
      fetchMembers();
      fetchUsers();
    }
  }, [isOpen, boardId]);

  const fetchMembers = async () => {
    try {
      const data = await boardApi.getMembers(boardId);
      setMembers(data);
    } catch {
      setError('Failed to load members');
    }
  };

  const fetchUsers = async () => {
    try {
      const data = await userApi.listUsers();
      setAllUsers(data);
    } catch {
      // Non-critical — search just won't work
    }
  };

  const handleAddMember = async (userId: number) => {
    setLoading(true);
    setError('');
    try {
      const member = await boardApi.addMember(boardId, userId, selectedRole);
      setMembers((prev) => {
        const existing = prev.findIndex((m) => m.userId === member.userId);
        if (existing >= 0) {
          const updated = [...prev];
          updated[existing] = member;
          return updated;
        }
        return [...prev, member];
      });
      setSearchQuery('');
    } catch (err: any) {
      setError(err?.response?.data?.message || err?.message || 'Failed to add member');
    } finally {
      setLoading(false);
    }
  };

  const handleRemoveMember = async (userId: number) => {
    if (!window.confirm('Remove this member from the board?')) return;
    setLoading(true);
    setError('');
    try {
      await boardApi.removeMember(boardId, userId);
      setMembers((prev) => prev.filter((m) => m.userId !== userId));
    } catch (err: any) {
      setError(err?.response?.data?.message || err?.message || 'Failed to remove member');
    } finally {
      setLoading(false);
    }
  };

  const handleChangeRole = async (userId: number, newRole: string) => {
    setLoading(true);
    setError('');
    try {
      const updated = await boardApi.addMember(boardId, userId, newRole);
      setMembers((prev) => prev.map((m) => (m.userId === updated.userId ? updated : m)));
    } catch (err: any) {
      setError(err?.response?.data?.message || err?.message || 'Failed to update role');
    } finally {
      setLoading(false);
    }
  };

  const memberUserIds = new Set(members.map((m) => m.userId));
  const nonMembers = allUsers.filter(
    (u) => !memberUserIds.has(u.id) && (
      searchQuery.trim() === '' ? false :
      u.username.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (u.fullName && u.fullName.toLowerCase().includes(searchQuery.toLowerCase()))
    )
  );

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Board Members" size="md">
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-3 py-2 rounded-lg mb-4 text-sm">
          {error}
        </div>
      )}

      {/* Add member section — only for board owner */}
      {isOwner && (
        <div className="mb-5 p-3 bg-surface-50 rounded-xl space-y-3">
          <p className="text-sm font-semibold text-surface-700">Add Member</p>
          <div className="flex gap-2">
            <div className="relative flex-1">
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search by username or name..."
                className="w-full px-3 py-2 text-sm border border-surface-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              />
            </div>
            <select
              value={selectedRole}
              onChange={(e) => setSelectedRole(e.target.value)}
              className="px-2 py-2 text-sm border border-surface-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
            >
              <option value="MEMBER">Member</option>
              <option value="EDITOR">Editor</option>
            </select>
          </div>

          {/* Search results */}
          {searchQuery.trim() !== '' && (
            <div className="max-h-36 overflow-y-auto space-y-1">
              {nonMembers.length === 0 ? (
                <p className="text-xs text-surface-400 py-2 text-center">No matching users found</p>
              ) : (
                nonMembers.map((user) => (
                  <div
                    key={user.id}
                    className="flex items-center justify-between px-3 py-2 hover:bg-surface-100 rounded-lg transition-colors"
                  >
                    <div className="flex items-center gap-2 min-w-0">
                      <div className="w-7 h-7 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center text-xs font-semibold flex-shrink-0">
                        {user.username.charAt(0).toUpperCase()}
                      </div>
                      <div className="min-w-0">
                        <p className="text-sm font-medium text-surface-800 truncate">{user.username}</p>
                        {user.fullName && (
                          <p className="text-xs text-surface-400 truncate">{user.fullName}</p>
                        )}
                      </div>
                    </div>
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={() => handleAddMember(user.id)}
                      loading={loading}
                    >
                      Add
                    </Button>
                  </div>
                ))
              )}
            </div>
          )}
        </div>
      )}

      {/* Current members list */}
      <div className="space-y-1 max-h-80 overflow-y-auto">
        {members.length === 0 ? (
          <p className="text-sm text-surface-400 text-center py-6">No members yet</p>
        ) : (
          members
            .sort((a, b) => {
              const order: Record<string, number> = { OWNER: 0, EDITOR: 1, MEMBER: 2 };
              return (order[a.role] ?? 3) - (order[b.role] ?? 3);
            })
            .map((member) => {
              const roleInfo = ROLE_LABELS[member.role] || ROLE_LABELS.MEMBER;
              const isSelf = member.userId === currentUserId;

              return (
                <div
                  key={member.userId}
                  className="flex items-center justify-between px-3 py-2.5 hover:bg-surface-50 rounded-lg transition-colors"
                >
                  <div className="flex items-center gap-3 min-w-0">
                    <div className="w-8 h-8 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center text-sm font-semibold flex-shrink-0">
                      {member.username.charAt(0).toUpperCase()}
                    </div>
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <p className="text-sm font-medium text-surface-800 truncate">
                          {member.username}
                          {isSelf && <span className="text-surface-400 font-normal"> (you)</span>}
                        </p>
                      </div>
                      {member.fullName && (
                        <p className="text-xs text-surface-400 truncate">{member.fullName}</p>
                      )}
                    </div>
                  </div>

                  <div className="flex items-center gap-2 flex-shrink-0">
                    {/* Role badge or dropdown */}
                    {isOwner && member.role !== 'OWNER' ? (
                      <select
                        value={member.role}
                        onChange={(e) => handleChangeRole(member.userId, e.target.value)}
                        className="text-xs px-2 py-1 border border-surface-200 rounded-lg bg-white focus:outline-none focus:ring-1 focus:ring-primary-500"
                      >
                        <option value="EDITOR">Editor</option>
                        <option value="MEMBER">Member</option>
                      </select>
                    ) : (
                      <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${roleInfo.color}`}>
                        {roleInfo.label}
                      </span>
                    )}

                    {/* Remove button — owner can remove non-owners */}
                    {isOwner && member.role !== 'OWNER' && (
                      <button
                        onClick={() => handleRemoveMember(member.userId)}
                        className="p-1 text-surface-400 hover:text-red-600 rounded transition-colors"
                        title="Remove member"
                      >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M6 18L18 6M6 6l12 12"
                          />
                        </svg>
                      </button>
                    )}
                  </div>
                </div>
              );
            })
        )}
      </div>
    </Modal>
  );
};
