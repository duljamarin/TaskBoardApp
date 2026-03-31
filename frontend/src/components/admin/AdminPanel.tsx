import React, { useEffect, useState } from 'react';
import { adminApi } from '../../api';
import { UserDTO } from '../../types';
import { Button } from '../common/Button';
import { useAuthStore } from '../../store/authStore';

const ROLE_LABELS: Record<string, { label: string; color: string; bg: string; border: string }> = {
  ROLE_ADMIN: { label: 'Admin', color: 'text-red-700', bg: 'bg-red-50', border: 'border-red-200' },
  ROLE_MODERATOR: { label: 'Moderator', color: 'text-amber-700', bg: 'bg-amber-50', border: 'border-amber-200' },
  ROLE_USER: { label: 'User', color: 'text-primary-700', bg: 'bg-primary-50', border: 'border-primary-200' },
};

export const AdminPanel: React.FC = () => {
  const [users, setUsers] = useState<UserDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState<number | null>(null);
  const currentUserId = useAuthStore((state) => state.userId);

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const data = await adminApi.getAllUsers();
      setUsers(data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  const handleAssignRole = async (userId: number, roleName: string) => {
    try {
      setActionLoading(userId);
      const updated = await adminApi.assignRole(userId, { roleName });
      setUsers((prev) => prev.map((u) => (u.id === userId ? updated : u)));
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to assign role');
    } finally {
      setActionLoading(null);
    }
  };

  const handleRemoveRole = async (userId: number, roleName: string) => {
    try {
      setActionLoading(userId);
      const updated = await adminApi.removeRole(userId, roleName);
      setUsers((prev) => prev.map((u) => (u.id === userId ? updated : u)));
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to remove role');
    } finally {
      setActionLoading(null);
    }
  };

  const handleToggleStatus = async (userId: number) => {
    try {
      setActionLoading(userId);
      const updated = await adminApi.toggleUserStatus(userId);
      setUsers((prev) => prev.map((u) => (u.id === userId ? updated : u)));
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to toggle status');
    } finally {
      setActionLoading(null);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[60vh]">
        <div className="flex flex-col items-center gap-3">
          <div className="w-10 h-10 border-3 border-primary-200 border-t-primary-600 rounded-full animate-spin" />
          <span className="text-sm text-surface-500 font-medium">Loading users…</span>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 animate-fade-in">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-surface-900">Admin Panel</h1>
        <p className="text-surface-500 text-sm mt-1">
          Manage users, assign roles, and control access
        </p>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-xl mb-6 text-sm">
          {error}
        </div>
      )}

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
        <div className="bg-white rounded-2xl border border-surface-100 p-5 shadow-card">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-primary-50 flex items-center justify-center">
              <svg className="w-5 h-5 text-primary-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m12 5.197v-1a6 6 0 00-6-6" />
              </svg>
            </div>
            <div>
              <p className="text-2xl font-bold text-surface-900">{users.length}</p>
              <p className="text-xs text-surface-500">Total Users</p>
            </div>
          </div>
        </div>
        <div className="bg-white rounded-2xl border border-surface-100 p-5 shadow-card">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-red-50 flex items-center justify-center">
              <svg className="w-5 h-5 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
              </svg>
            </div>
            <div>
              <p className="text-2xl font-bold text-surface-900">
                {users.filter((u) => u.roles.includes('ROLE_ADMIN')).length}
              </p>
              <p className="text-xs text-surface-500">Admins</p>
            </div>
          </div>
        </div>
        <div className="bg-white rounded-2xl border border-surface-100 p-5 shadow-card">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-green-50 flex items-center justify-center">
              <svg className="w-5 h-5 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div>
              <p className="text-2xl font-bold text-surface-900">
                {users.filter((u) => u.active).length}
              </p>
              <p className="text-xs text-surface-500">Active Users</p>
            </div>
          </div>
        </div>
      </div>

      {/* User Table */}
      <div className="bg-white rounded-2xl border border-surface-100 shadow-card overflow-hidden">
        <div className="px-6 py-4 border-b border-surface-100 flex items-center justify-between">
          <h2 className="text-base font-semibold text-surface-900">All Users</h2>
          <Button variant="ghost" size="sm" onClick={fetchUsers}>
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            Refresh
          </Button>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="text-left text-xs font-medium text-surface-500 uppercase tracking-wider bg-surface-50">
                <th className="px-6 py-3">User</th>
                <th className="px-6 py-3">Roles</th>
                <th className="px-6 py-3">Status</th>
                <th className="px-6 py-3">Joined</th>
                <th className="px-6 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-surface-100">
              {users.map((user) => (
                <tr key={user.id} className="hover:bg-surface-50/50 transition-colors">
                  {/* User info */}
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <div className="w-9 h-9 rounded-full bg-gradient-to-br from-primary-400 to-accent-400 flex items-center justify-center text-white text-sm font-bold uppercase flex-shrink-0">
                        {user.username.charAt(0)}
                      </div>
                      <div>
                        <p className="text-sm font-semibold text-surface-900">
                          {user.fullName || user.username}
                          {user.id === currentUserId && (
                            <span className="ml-1.5 text-xs font-normal text-surface-400">(you)</span>
                          )}
                        </p>
                        <p className="text-xs text-surface-500">{user.email}</p>
                      </div>
                    </div>
                  </td>

                  {/* Roles */}
                  <td className="px-6 py-4">
                    <div className="flex flex-wrap gap-1.5">
                      {user.roles.map((role) => {
                        const style = ROLE_LABELS[role] || ROLE_LABELS.ROLE_USER;
                        return (
                          <span
                            key={role}
                            className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ${style.bg} ${style.color} border ${style.border}`}
                          >
                            {style.label}
                            {role !== 'ROLE_USER' && user.id !== currentUserId && (
                              <button
                                onClick={() => handleRemoveRole(user.id, role)}
                                className="ml-0.5 hover:opacity-70"
                                title={`Remove ${style.label}`}
                              >
                                ×
                              </button>
                            )}
                          </span>
                        );
                      })}
                    </div>
                  </td>

                  {/* Status */}
                  <td className="px-6 py-4">
                    <span
                      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${
                        user.active
                          ? 'bg-green-50 text-green-700 border border-green-200'
                          : 'bg-surface-100 text-surface-500 border border-surface-200'
                      }`}
                    >
                      <span
                        className={`w-1.5 h-1.5 rounded-full ${user.active ? 'bg-green-500' : 'bg-surface-400'}`}
                      />
                      {user.active ? 'Active' : 'Disabled'}
                    </span>
                  </td>

                  {/* Joined date */}
                  <td className="px-6 py-4 text-xs text-surface-500">
                    {user.createdAt
                      ? new Date(user.createdAt).toLocaleDateString('en-US', {
                          month: 'short',
                          day: 'numeric',
                          year: 'numeric',
                        })
                      : '—'}
                  </td>

                  {/* Actions */}
                  <td className="px-6 py-4">
                    <div className="flex items-center justify-end gap-2">
                      {actionLoading === user.id ? (
                        <div className="w-5 h-5 border-2 border-primary-200 border-t-primary-600 rounded-full animate-spin" />
                      ) : (
                        <>
                          {/* Assign role dropdown */}
                          {!user.roles.includes('ROLE_ADMIN') && user.id !== currentUserId && (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleAssignRole(user.id, 'ROLE_ADMIN')}
                              title="Make Admin"
                            >
                              <svg className="w-4 h-4 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                              </svg>
                            </Button>
                          )}
                          {!user.roles.includes('ROLE_MODERATOR') && user.id !== currentUserId && (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleAssignRole(user.id, 'ROLE_MODERATOR')}
                              title="Make Moderator"
                            >
                              <svg className="w-4 h-4 text-amber-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
                              </svg>
                            </Button>
                          )}
                          {user.id !== currentUserId && (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleToggleStatus(user.id)}
                              title={user.active ? 'Disable user' : 'Enable user'}
                            >
                              {user.active ? (
                                <svg className="w-4 h-4 text-surface-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
                                </svg>
                              ) : (
                                <svg className="w-4 h-4 text-green-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                                </svg>
                              )}
                            </Button>
                          )}
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

