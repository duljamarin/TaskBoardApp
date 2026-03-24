import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { authApi } from '../../api';
import { Button } from '../common/Button';
import { Input } from '../common/Input';

export const Register: React.FC = () => {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    fullName: '',
  });
  const [error, setError] = useState('');
  const [conflictError, setConflictError] = useState(false);
  const [loading, setLoading] = useState(false);

  const login = useAuthStore((state) => state.login);
  const navigate = useNavigate();

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setConflictError(false);
    setError('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setConflictError(false);

    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    setLoading(true);

    try {
      const response = await authApi.register({
        username: formData.username,
        email: formData.email,
        password: formData.password,
        fullName: formData.fullName || undefined,
      });
      login(response);
      navigate('/boards');
    } catch (err: any) {
      const message: string = err.response?.data?.message ?? '';
      if (message.toLowerCase().includes('username already exists') ||
          message.toLowerCase().includes('email already exists')) {
        setConflictError(true);
        setError(message);
      } else {
        setError(message || 'Registration failed. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
            Create your account
          </h2>
        </div>
        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          {error && (
            <div className="bg-red-50 border border-red-400 text-red-700 px-4 py-3 rounded">
              {error}
              {conflictError && (
                <span>
                  {' '}Already have an account?{' '}
                  <Link to="/login" className="font-semibold underline hover:text-red-900">
                    Sign in
                  </Link>
                </span>
              )}
            </div>
          )}
          <div className="space-y-4">
            <Input
              label="Username"
              type="text"
              name="username"
              value={formData.username}
              onChange={handleChange}
              required
              placeholder="Choose a username"
            />
            <Input
              label="Email"
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              required
              placeholder="Enter your email"
            />
            <Input
              label="Full Name (Optional)"
              type="text"
              name="fullName"
              value={formData.fullName}
              onChange={handleChange}
              placeholder="Enter your full name"
            />
            <Input
              label="Password"
              type="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              required
              placeholder="Choose a password"
            />
            <Input
              label="Confirm Password"
              type="password"
              name="confirmPassword"
              value={formData.confirmPassword}
              onChange={handleChange}
              required
              placeholder="Confirm your password"
            />
          </div>

          <div>
            <Button
              type="submit"
              variant="primary"
              className="w-full"
              loading={loading}
            >
              Register
            </Button>
          </div>

          <div className="text-center">
            <span className="text-gray-600">Already have an account? </span>
            <Link to="/login" className="text-primary-600 hover:text-primary-500 font-medium">
              Sign in
            </Link>
          </div>
        </form>
      </div>
    </div>
  );
};

