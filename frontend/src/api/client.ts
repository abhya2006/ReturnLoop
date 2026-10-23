import { apiRequest, buildQuery } from '@/lib/api';
import type {
  AgingItem,
  AuditEvent,
  DashboardSummary,
  Driver,
  EnvironmentalSummary,
  ImportReport,
  Inspection,
  InspectionStatus,
  InventoryItem,
  InventoryStatus,
  JobStatus,
  Location,
  LoginResponse,
  MapMarker,
  Notification,
  Organization,
  PageResponse,
  Partner,
  PartnerMatch,
  PickupJob,
  PickupStatus,
  Product,
  ProductCondition,
  RecoveryEvaluation,
  RecoveryJob,
  RecoveryPath,
  RecoveryRecommendation,
  ReportSummary,
  ReturnDetail,
  ReturnReason,
  ReturnStatus,
  ReturnSummary,
  Route,
  UserProfile,
  Vehicle,
} from '@/lib/types';

/* ------------------------------------------------------------------ auth */

export const authApi = {
  login: (email: string, password: string) =>
    apiRequest<LoginResponse>('/api/v1/auth/login', {
      method: 'POST',
      body: { email, password },
    }),
  me: () => apiRequest<UserProfile>('/api/v1/auth/me'),
  register: (body: { organizationName: string; fullName: string; email: string; password: string }) =>
    apiRequest<LoginResponse>('/api/v1/auth/register', { method: 'POST', body }),
  requestPasswordReset: (email: string) =>
    apiRequest<{ message: string }>('/api/v1/auth/forgot-password', { method: 'POST', body: { email } }),
  verifyPasswordResetCode: (email: string, code: string) =>
    apiRequest<{ message: string }>('/api/v1/auth/verify-reset-code', { method: 'POST', body: { email, code } }),
  resetPassword: (email: string, code: string, newPassword: string) =>
    apiRequest<{ message: string }>('/api/v1/auth/reset-password', { method: 'POST', body: { email, code, newPassword } }),
};

/* ------------------------------------------------------------- catalogue */

export const catalogApi = {
  search: (params: {
    search?: string;
    category?: string;
    page?: number;
    size?: number;
  }) => apiRequest<PageResponse<Product>>(`/api/v1/products${buildQuery(params)}`),
  create: (body: Partial<Product> & { sku: string; name: string }) =>
    apiRequest<Product>('/api/v1/products', { method: 'POST', body }),
  update: (id: string, body: Partial<Product> & { sku: string; name: string }) =>
    apiRequest<Product>(`/api/v1/products/${id}`, { method: 'PUT', body }),
};

/* ---------------------------------------------------------------- returns */

export interface CreateReturnItemInput {
  sku: string;
  quantity: number;
  condition?: ProductCondition;
  serialNumber?: string | null;
  accessoriesComplete?: boolean;
  notes?: string | null;
}

export interface CreateReturnInput {
  reason: ReturnReason;
  sourceReference?: string | null;
  customerName?: string | null;
  customerEmail?: string | null;
  originAddress?: string | null;
  originLatitude?: number | null;
  originLongitude?: number | null;
  warehouseId?: string | null;
  notes?: string | null;
  items: CreateReturnItemInput[];
}

export const returnsApi = {
  search: (params: {
    search?: string;
    status?: ReturnStatus[];
    reason?: ReturnReason;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
    sort?: string;
  }) => apiRequest<PageResponse<ReturnSummary>>(`/api/v1/returns${buildQuery(params)}`),
  get: (id: string) => apiRequest<ReturnDetail>(`/api/v1/returns/${id}`),
  create: (body: CreateReturnInput, idempotencyKey?: string) =>
    apiRequest<ReturnDetail>('/api/v1/returns', {
      method: 'POST',
      body,
      headers: idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : {},
    }),
  addItem: (id: string, body: CreateReturnItemInput) =>
    apiRequest<ReturnDetail>(`/api/v1/returns/${id}/items`, { method: 'POST', body }),
  transition: (id: string, status: ReturnStatus, reason?: string) =>
    apiRequest<ReturnDetail>(`/api/v1/returns/${id}/status`, {
      method: 'POST',
      body: { status, reason },
    }),
  cancel: (id: string, reason: string) =>
    apiRequest<ReturnDetail>(`/api/v1/returns/${id}/cancel`, { method: 'POST', body: { reason } }),
  assignWarehouse: (id: string, warehouseId: string) =>
    apiRequest<ReturnDetail>(`/api/v1/returns/${id}/warehouse`, {
      method: 'POST',
      body: { warehouseId },
    }),
  setEstimatedValue: (id: string, value: number) =>
    apiRequest<ReturnDetail>(`/api/v1/returns/${id}/estimated-value`, {
      method: 'POST',
      body: { value },
    }),
  setRealizedValue: (id: string, value: number) =>
    apiRequest<ReturnDetail>(`/api/v1/returns/${id}/realized-value`, {
      method: 'POST',
      body: { value },
    }),
};

/* -------------------------------------------------------------- recovery */

export const recoveryApi = {
  preview: (returnId: string) =>
    apiRequest<RecoveryEvaluation>(`/api/v1/recovery/returns/${returnId}/options`),
  recommend: (returnId: string) =>
    apiRequest<RecoveryEvaluation>(`/api/v1/recovery/returns/${returnId}/recommend`, {
      method: 'POST',
    }),
  recommendations: (returnId: string) =>
    apiRequest<RecoveryRecommendation[]>(`/api/v1/recovery/returns/${returnId}/recommendations`),
  decide: (id: string, approve: boolean, reason?: string) =>
    apiRequest<RecoveryRecommendation>(`/api/v1/recovery/recommendations/${id}/decision`, {
      method: 'POST',
      body: { approve, reason },
    }),
};

/* ------------------------------------------------------------ inspection */

export const inspectionApi = {
  search: (params: { status?: InspectionStatus; returnId?: string; page?: number; size?: number }) =>
    apiRequest<PageResponse<Inspection>>(`/api/v1/inspections${buildQuery(params)}`),
  get: (id: string) => apiRequest<Inspection>(`/api/v1/inspections/${id}`),
  open: (returnItemId: string) =>
    apiRequest<Inspection>(`/api/v1/inspections/items/${returnItemId}`, { method: 'POST' }),
  record: (
    id: string,
    body: {
      packagingCondition?: ProductCondition;
      functionalityResult?: ProductCondition;
      functional?: boolean;
      accessoriesComplete?: boolean;
      damageDescription?: string;
      missingAccessories?: string;
      estimatedRepairCost?: number;
      notes?: string;
      status?: InspectionStatus;
      flaggedUnsafe?: boolean;
    },
  ) =>
    apiRequest<Inspection>(`/api/v1/inspections/${id}/record`, { method: 'POST', body }),
  complete: (id: string) =>
    apiRequest<Inspection>(`/api/v1/inspections/${id}/complete`, { method: 'POST' }),
  addAttachment: (
    id: string,
    body: { fileName: string; contentType: string; sizeBytes: number; caption?: string },
  ) =>
    apiRequest<Inspection>(`/api/v1/inspections/${id}/attachments`, { method: 'POST', body }),
};

/* -------------------------------------------------------------- inventory */

export const inventoryApi = {
  search: (params: {
    search?: string;
    status?: InventoryStatus;
    category?: string;
    warehouseId?: string;
    page?: number;
    size?: number;
  }) => apiRequest<PageResponse<InventoryItem>>(`/api/v1/inventory${buildQuery(params)}`),
  get: (id: string) => apiRequest<InventoryItem & { movements: unknown[] }>(`/api/v1/inventory/${id}`),
  aging: (thresholdDays = 30) =>
    apiRequest<AgingItem[]>(`/api/v1/inventory/aging${buildQuery({ thresholdDays })}`),
  receive: (body: {
    returnId?: string;
    sku: string;
    productName?: string;
    category?: string;
    quantity: number;
    condition?: ProductCondition;
    warehouseId?: string;
    locationCode?: string;
    unitValue?: number;
    weightKg?: number;
  }) => apiRequest<InventoryItem>('/api/v1/inventory/receive', { method: 'POST', body }),
  reserve: (id: string, quantity: number, referenceId?: string) =>
    apiRequest<InventoryItem>(`/api/v1/inventory/${id}/reserve`, {
      method: 'POST',
      body: { quantity, referenceId },
    }),
  release: (id: string, quantity: number, referenceId?: string) =>
    apiRequest<InventoryItem>(`/api/v1/inventory/${id}/release`, {
      method: 'POST',
      body: { quantity, referenceId },
    }),
  dispatch: (id: string, quantity: number, referenceId?: string) =>
    apiRequest<InventoryItem>(`/api/v1/inventory/${id}/dispatch`, {
      method: 'POST',
      body: { quantity, referenceId },
    }),
  adjust: (id: string, delta: number, reason?: string) =>
    apiRequest<InventoryItem>(`/api/v1/inventory/${id}/adjust`, {
      method: 'POST',
      body: { delta, reason },
    }),
};

/* --------------------------------------------------------------- partners */

export const partnerApi = {
  search: (params: {
    search?: string;
    type?: string;
    category?: string;
    region?: string;
    availableOnly?: boolean;
    page?: number;
    size?: number;
  }) => apiRequest<PageResponse<Partner>>(`/api/v1/partners${buildQuery(params)}`),
  get: (id: string) => apiRequest<Partner & { recentJobs: RecoveryJob[] }>(`/api/v1/partners/${id}`),
  match: (path: RecoveryPath, category?: string, region?: string) =>
    apiRequest<PartnerMatch[]>(
      `/api/v1/partners/match${buildQuery({ path, category, region })}`,
    ),
  assign: (body: {
    inventoryItemId?: string;
    returnId?: string;
    returnItemId?: string;
    sku?: string;
    productName?: string;
    quantity?: number;
    recoveryPath: RecoveryPath;
    expectedRecoveryValue?: number;
    serviceDeadline?: string;
    partnerId: string;
  }) => apiRequest<RecoveryJob>('/api/v1/partners/jobs', { method: 'POST', body }),
};

export const jobApi = {
  search: (params: {
    search?: string;
    status?: JobStatus[];
    partnerId?: string;
    path?: RecoveryPath;
    page?: number;
    size?: number;
  }) => apiRequest<PageResponse<RecoveryJob>>(`/api/v1/partner-jobs${buildQuery(params)}`),
  get: (id: string) => apiRequest<RecoveryJob & { history: unknown[] }>(`/api/v1/partner-jobs/${id}`),
  transition: (id: string, status: JobStatus, note?: string, realizedValue?: number) =>
    apiRequest<RecoveryJob>(`/api/v1/partner-jobs/${id}/status`, {
      method: 'POST',
      body: { status, note, realizedValue },
    }),
};

/* -------------------------------------------------------------- logistics */

export const logisticsApi = {
  pickups: (params: {
    status?: PickupStatus[];
    date?: string;
    vehicleId?: string;
    page?: number;
    size?: number;
  }) => apiRequest<PageResponse<PickupJob>>(`/api/v1/logistics/pickups${buildQuery(params)}`),
  pickup: (id: string) => apiRequest<PickupJob>(`/api/v1/logistics/pickups/${id}`),
  createPickup: (body: {
    returnId?: string;
    originAddress?: string;
    originLatitude?: number;
    originLongitude?: number;
    destinationLocationId?: string;
    scheduledDate?: string;
    windowStart?: string;
    windowEnd?: string;
    priority?: number;
    unitCount?: number;
    totalWeightKg?: number;
    notes?: string;
  }) => apiRequest<PickupJob>('/api/v1/logistics/pickups', { method: 'POST', body }),
  schedule: (
    id: string,
    body: {
      scheduledDate: string;
      windowStart?: string;
      windowEnd?: string;
      vehicleId?: string;
      driverId?: string;
      priority?: number;
    },
  ) => apiRequest<PickupJob>(`/api/v1/logistics/pickups/${id}/schedule`, { method: 'POST', body }),
  updatePickupStatus: (id: string, status: PickupStatus, note?: string) =>
    apiRequest<PickupJob>(`/api/v1/logistics/pickups/${id}/status`, {
      method: 'POST',
      body: { status, note },
    }),
  reassign: (id: string, vehicleId?: string, driverId?: string) =>
    apiRequest<PickupJob>(`/api/v1/logistics/pickups/${id}/reassign`, {
      method: 'POST',
      body: { vehicleId, driverId },
    }),
  optimize: (date: string, depotLocationId?: string, includeAssigned = false) =>
    apiRequest<Route[]>(`/api/v1/logistics/routes/optimize${buildQuery({ date })}`, {
      method: 'POST',
      body: { depotLocationId, includeAssigned },
    }),
  routes: (params: { status?: string; date?: string; page?: number; size?: number }) =>
    apiRequest<PageResponse<Route>>(`/api/v1/logistics/routes${buildQuery(params)}`),
  route: (id: string) => apiRequest<Route>(`/api/v1/logistics/routes/${id}`),
  updateRouteStatus: (id: string, status: string) =>
    apiRequest<Route>(`/api/v1/logistics/routes/${id}/status`, { method: 'POST', body: { status } }),
  completeStop: (routeId: string, stopId: string, note?: string) =>
    apiRequest<Route>(`/api/v1/logistics/routes/${routeId}/stops/${stopId}/complete`, {
      method: 'POST',
      body: { note },
    }),
  locations: () => apiRequest<Location[]>('/api/v1/logistics/locations'),
  vehicles: () => apiRequest<Vehicle[]>('/api/v1/logistics/vehicles'),
  drivers: () => apiRequest<Driver[]>('/api/v1/logistics/drivers'),
};

/* -------------------------------------------------------------- analytics */

export const analyticsApi = {
  dashboard: (params: { from?: string; to?: string }) =>
    apiRequest<DashboardSummary>(`/api/v1/analytics/dashboard${buildQuery(params)}`),
  report: (params: { from?: string; to?: string }) =>
    apiRequest<ReportSummary>(`/api/v1/analytics/reports${buildQuery(params)}`),
  environmental: (params: { from?: string; to?: string }) =>
    apiRequest<EnvironmentalSummary>(`/api/v1/analytics/environmental${buildQuery(params)}`),
};

/* ------------------------------------------------- notifications & audit */

export const notificationApi = {
  list: (params: { page?: number; size?: number }) =>
    apiRequest<PageResponse<Notification>>(`/api/v1/notifications${buildQuery(params)}`),
  unreadCount: () => apiRequest<number>('/api/v1/notifications/unread-count'),
  markRead: (id: string) =>
    apiRequest<Notification>(`/api/v1/notifications/${id}/read`, { method: 'POST' }),
  markAllRead: () => apiRequest<number>('/api/v1/notifications/read-all', { method: 'POST' }),
};

export const auditApi = {
  search: (params: { action?: string; entityType?: string; page?: number; size?: number }) =>
    apiRequest<PageResponse<AuditEvent>>(`/api/v1/audit${buildQuery(params)}`),
};

export const organizationApi = {
  current: () => apiRequest<Organization>('/api/v1/organizations/current'),
  update: (body: Partial<Organization>) =>
    apiRequest<Organization>('/api/v1/organizations/current', { method: 'PUT', body }),
};

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: string;
  roleLabel: string;
  jobTitle: string | null;
  active: boolean;
  lastLoginAt: string | null;
  createdAt: string;
}

export const usersApi = {
  list: () => apiRequest<User[]>('/api/v1/users'),
  create: (body: {
    email: string;
    fullName: string;
    password: string;
    role: string;
    jobTitle?: string;
  }) => apiRequest<User>('/api/v1/users', { method: 'POST', body }),
  update: (
    id: string,
    body: { fullName?: string; role?: string; jobTitle?: string; active?: boolean; password?: string },
  ) => apiRequest<User>(`/api/v1/users/${id}`, { method: 'PUT', body }),
  deactivate: (id: string) => apiRequest<void>(`/api/v1/users/${id}/deactivate`, { method: 'POST' }),
};

/* ----------------------------------------------------------------- import */

export const importApi = {
  returns: (file: File) => {
    const form = new FormData();
    form.append('file', file);
    return apiRequest<ImportReport>('/api/v1/imports/returns', { method: 'POST', body: form });
  },
  inventory: (file: File) => {
    const form = new FormData();
    form.append('file', file);
    return apiRequest<ImportReport>('/api/v1/imports/inventory', { method: 'POST', body: form });
  },
};

export type { MapMarker };
