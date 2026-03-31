import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  DndContext,
  DragEndEvent,
  DragOverEvent,
  DragOverlay,
  DragStartEvent,
  PointerSensor,
  useSensor,
  useSensors,
  closestCorners,
} from '@dnd-kit/core';
import { useBoardStore } from '../../store/boardStore';
import { useAuthStore } from '../../store/authStore';
import { Button } from '../common/Button';
import { ListView } from '../lists/ListView';
import { CreateListForm } from '../lists/CreateListForm';
import { Card } from '../../types';
import { websocketService, WebSocketMessage } from '../../services/websocket';
import { LabelManager } from '../labels/LabelManager';
import { LabelBadge } from '../labels/LabelBadge';

export const BoardView: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { currentBoard, loading, error, fetchBoard, deleteBoard, moveCard, moveCardLocally } = useBoardStore();
  const { accessToken, username } = useAuthStore();
  const [showCreateList, setShowCreateList] = useState(false);
  const [activeCard, setActiveCard] = useState<Card | null>(null);
  const [wsConnected, setWsConnected] = useState(false);
  const [realtimeUpdates, setRealtimeUpdates] = useState<string[]>([]);
  const [showLabelManager, setShowLabelManager] = useState(false);
  const [filterLabelIds, setFilterLabelIds] = useState<Set<number>>(new Set());

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 8,
      },
    })
  );

  useEffect(() => {
    if (id) {
      fetchBoard(parseInt(id, 10));
    }
  }, [id, fetchBoard]);

  // WebSocket subscription effect
  useEffect(() => {
    const boardId = id ? parseInt(id, 10) : null;

    if (!boardId || !currentBoard || !accessToken) {
      return;
    }

    // Ensure WebSocket is connected
    if (!websocketService.isConnected()) {
      websocketService.connect(accessToken)
        .then(() => {
          setWsConnected(true);
          subscribeToUpdates(boardId);
        })
        .catch((error) => {
          console.error('Failed to connect WebSocket:', error);
        });
    } else {
      setWsConnected(true);
      subscribeToUpdates(boardId);
    }

    function subscribeToUpdates(validBoardId: number) {
      websocketService.subscribeToBoard(validBoardId, (message) => handleWebSocketMessage(message, validBoardId));
    }

    function handleWebSocketMessage(message: WebSocketMessage, validBoardId: number) {
      console.log('📨 WebSocket update received:', message);

      // Add notification
      if (message.username && message.username !== username) {
        const notification = `${message.username} ${getActionText(message.type)}`;
        setRealtimeUpdates(prev => [notification, ...prev.slice(0, 4)]);
        setTimeout(() => {
          setRealtimeUpdates(prev => prev.filter(n => n !== notification));
        }, 5000);
      }

      switch (message.type) {
        case 'CARD_MOVED':
        case 'CARD_MOVE_BROADCAST':
          if (message.data?.card) {
            const { card, fromListId, toListId } = message.data;
            // Only update if it's from another user
            if (message.userId !== useAuthStore.getState().userId) {
              moveCardLocally(card.id, fromListId, toListId, card.position);
            }
          }
          break;

        case 'CARD_CREATED':
        case 'CARD_UPDATED':
        case 'CARD_DELETED':
        case 'LIST_CREATED':
        case 'LIST_UPDATED':
        case 'LIST_DELETED':
          // Refetch board data for these events
          if (message.userId !== useAuthStore.getState().userId) {
            fetchBoard(validBoardId);
          }
          break;

        case 'SUBSCRIPTION_ACK':
          console.log('✅ Subscription confirmed:', message);
          break;

        default:
          console.log('Unknown message type:', message.type);
      }
    }

    function getActionText(type: string): string {
      switch (type) {
        case 'CARD_MOVED':
        case 'CARD_MOVE_BROADCAST':
          return 'moved a card';
        case 'CARD_CREATED':
          return 'created a card';
        case 'CARD_UPDATED':
          return 'updated a card';
        case 'CARD_DELETED':
          return 'deleted a card';
        case 'LIST_CREATED':
          return 'created a list';
        case 'LIST_UPDATED':
          return 'updated a list';
        case 'LIST_DELETED':
          return 'deleted a list';
        default:
          return 'made a change';
      }
    }

    // Cleanup: unsubscribe when component unmounts or board changes
    return () => {
      if (boardId) {
        websocketService.unsubscribeFromBoard(boardId);
      }
    };
  }, [id, currentBoard?.id, accessToken, username]);

  const handleDeleteBoard = async () => {
    if (currentBoard && window.confirm('Are you sure you want to delete this board?')) {
      try {
        await deleteBoard(currentBoard.id);
        navigate('/boards');
      } catch (error) {
        console.error('Failed to delete board:', error);
      }
    }
  };

  const handleDragStart = (event: DragStartEvent) => {
    const { active } = event;
    const activeData = active.data.current;

    if (activeData?.type === 'card') {
      setActiveCard(activeData.card);
    }
  };

  const handleDragOver = (event: DragOverEvent) => {
    const { active, over } = event;
    if (!over) return;

    const activeId = active.id;
    const overId = over.id;

    if (activeId === overId) return;

    const activeData = active.data.current;
    const overData = over.data.current;

    if (!activeData || !currentBoard) return;

    // Moving card over another card or over a list
    if (activeData.type === 'card') {
      const activeCard = activeData.card;
      const activeListId = activeCard.listId;

      let targetListId: number;
      let targetPosition: number;

      if (overData?.type === 'card') {
        // Hovering over another card
        const overCard = overData.card;
        targetListId = overCard.listId;
        targetPosition = overCard.position;
      } else if (overData?.type === 'list') {
        // Hovering over a list
        const overList = overData.list;
        targetListId = overList.id;
        targetPosition = overList.cards.length;
      } else {
        return;
      }

      if (activeListId !== targetListId || activeCard.position !== targetPosition) {
        moveCardLocally(activeCard.id, activeListId, targetListId, targetPosition);
      }
    }
  };

  const handleDragEnd = async (event: DragEndEvent) => {
    const { active, over } = event;
    setActiveCard(null);

    if (!over || !currentBoard) return;

    const activeData = active.data.current;

    if (activeData?.type === 'card') {
      const activeCard = activeData.card;
      const overData = over.data.current;

      let targetListId: number;
      let targetPosition: number;

      if (overData?.type === 'card') {
        const overCard = overData.card;
        targetListId = overCard.listId;

        // Find the target position in the new list
        const targetList = currentBoard.lists.find(l => l.id === targetListId);
        if (targetList) {
          const cardIndex = targetList.cards.findIndex(c => c.id === activeCard.id);
          targetPosition = cardIndex >= 0 ? cardIndex : 0;
        } else {
          targetPosition = 0;
        }
      } else if (overData?.type === 'list') {
        const overList = overData.list;
        targetListId = overList.id;

        // Find the position in the list
        const targetList = currentBoard.lists.find(l => l.id === targetListId);
        if (targetList) {
          const cardIndex = targetList.cards.findIndex(c => c.id === activeCard.id);
          targetPosition = cardIndex >= 0 ? cardIndex : targetList.cards.length - 1;
        } else {
          targetPosition = 0;
        }
      } else {
        return;
      }

      // Only call API if the card actually moved
      if (activeCard.listId !== targetListId || activeCard.position !== targetPosition) {
        try {
          await moveCard(activeCard.id, {
            newListId: targetListId,
            newPosition: targetPosition,
          });
        } catch (error) {
          console.error('Failed to move card:', error);
          // Refetch to restore correct state
          if (id) {
            fetchBoard(parseInt(id, 10));
          }
        }
      }
    }
  };

  if (loading && !currentBoard) {
    return (
      <div className="flex justify-center items-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="bg-red-50 border border-red-400 text-red-700 px-4 py-3 rounded">
          {error}
        </div>
      </div>
    );
  }

  if (!currentBoard) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="text-center">
          <p className="text-gray-500">Board not found</p>
          <Button onClick={() => navigate('/boards')} className="mt-4">
            Back to Boards
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen" style={{ backgroundColor: currentBoard.color + '20' }}>
      <div className="bg-white shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex justify-between items-center">
            <div>
              <Button variant="ghost" size="sm" onClick={() => navigate('/boards')}>
                ← Back to Boards
              </Button>
              <h1 className="text-2xl font-bold text-gray-900 mt-2">{currentBoard.name}</h1>
              {currentBoard.description && (
                <p className="text-gray-600 mt-1">{currentBoard.description}</p>
              )}
            </div>
            <div className="flex items-center space-x-4">
              {/* WebSocket Status Indicator */}
              <div className="flex items-center space-x-2 text-sm">
                <div className={`w-2 h-2 rounded-full ${wsConnected ? 'bg-green-500' : 'bg-gray-400'}`}></div>
                <span className="text-gray-600">
                  {wsConnected ? 'Live' : 'Offline'}
                </span>
              </div>
              <Button variant="danger" onClick={handleDeleteBoard}>
                Delete Board
              </Button>
              <Button variant="secondary" onClick={() => setShowLabelManager(true)}>
                🏷 Labels
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Real-time Update Notifications */}
      {realtimeUpdates.length > 0 && (
        <div className="fixed top-20 right-4 z-50 space-y-2">
          {realtimeUpdates.map((update, index) => (
            <div
              key={index}
              className="bg-blue-500 text-white px-4 py-2 rounded-lg shadow-lg animate-fade-in"
            >
              🔄 {update}
            </div>
          ))}
        </div>
      )}

      <DndContext
        sensors={sensors}
        collisionDetection={closestCorners}
        onDragStart={handleDragStart}
        onDragOver={handleDragOver}
        onDragEnd={handleDragEnd}
      >
        {/* Label filter bar */}
        {currentBoard.lists.some((l) => l.cards.some((c) => c.labels && c.labels.length > 0)) && (
          <div className="px-4 pt-4">
            <div className="flex items-center gap-2 flex-wrap">
              <span className="text-sm font-medium text-gray-600">Filter by label:</span>
              {/* Collect all unique labels from the board */}
              {Array.from(
                new Map(
                  currentBoard.lists
                    .flatMap((l) => l.cards)
                    .flatMap((c) => c.labels || [])
                    .map((label) => [label.id, label])
                ).values()
              )
                .sort((a, b) => a.name.localeCompare(b.name))
                .map((label) => (
                  <button
                    key={label.id}
                    onClick={() =>
                      setFilterLabelIds((prev) => {
                        const next = new Set(prev);
                        if (next.has(label.id)) next.delete(label.id);
                        else next.add(label.id);
                        return next;
                      })
                    }
                    className={`transition-opacity ${
                      filterLabelIds.size > 0 && !filterLabelIds.has(label.id) ? 'opacity-40' : ''
                    }`}
                  >
                    <LabelBadge label={label} size="sm" />
                  </button>
                ))}
              {filterLabelIds.size > 0 && (
                <button
                  onClick={() => setFilterLabelIds(new Set())}
                  className="text-xs text-gray-500 hover:text-gray-700 underline ml-1"
                >
                  Clear filter
                </button>
              )}
            </div>
          </div>
        )}

        <div className="px-4 py-6">
          <div className="flex space-x-4 overflow-x-auto pb-4">
            {currentBoard.lists
              .sort((a, b) => a.position - b.position)
              .map((list) => {
                // Apply label filter: if any filter is active, only show cards matching the filter
                if (filterLabelIds.size > 0) {
                  const filteredCards = list.cards.filter((c) =>
                    c.labels && c.labels.some((l) => filterLabelIds.has(l.id))
                  );
                  return <ListView key={list.id} list={{ ...list, cards: filteredCards }} />;
                }
                return <ListView key={list.id} list={list} />;
              })}

            {showCreateList ? (
              <CreateListForm
                boardId={currentBoard.id}
                onCancel={() => setShowCreateList(false)}
                onSuccess={() => setShowCreateList(false)}
              />
            ) : (
              <button
                onClick={() => setShowCreateList(true)}
                className="flex-shrink-0 w-72 h-fit bg-gray-100 hover:bg-gray-200 rounded-lg p-4 text-gray-700 font-medium transition-colors"
              >
                + Add List
              </button>
            )}
          </div>
        </div>

        <DragOverlay>
          {activeCard ? (
            <div className="bg-white rounded-lg p-3 shadow-lg opacity-90 cursor-grabbing w-72">
              <h4 className="text-sm font-medium text-gray-900 mb-2">{activeCard.title}</h4>
              {activeCard.description && (
                <p className="text-xs text-gray-600 mb-2 line-clamp-2">{activeCard.description}</p>
              )}
            </div>
          ) : null}
        </DragOverlay>
      </DndContext>

      {/* Label Manager Modal */}
      <LabelManager
        isOpen={showLabelManager}
        onClose={() => {
          setShowLabelManager(false);
          // Refresh board to pick up any label changes
          if (id) fetchBoard(parseInt(id, 10));
        }}
        boardId={currentBoard.id}
      />
    </div>
  );
};

