const PRIORITY_PATH = '/api/staff/orders/dispatch?filter=PRIORITY';

export function hasPostConflictPriorityReload(events, conflictPath) {
  const conflict = events.find(event => event.phase === 'response' && event.method === 'PUT'
    && event.path === conflictPath && event.status === 409);
  if (!conflict) return false;
  return events.some(request => request.phase === 'request' && request.method === 'GET'
    && request.path === PRIORITY_PATH && request.sequence > conflict.sequence
    && events.some(response => response.phase === 'response' && response.id === request.id
      && response.status === 200 && response.sequence > request.sequence));
}
