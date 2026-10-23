export type ReturnStatus =
  | 'RETURN_CREATED'
  | 'PICKUP_SCHEDULED'
  | 'COLLECTED'
  | 'WAREHOUSE_RECEIVED'
  | 'INSPECTED'
  | 'RECOVERY_DECISION'
  | 'PARTNER_ASSIGNED'
  | 'PROCESSING'
  | 'RECOVERED'
  | 'COMPLETED'
  | 'CANCELLED';

export type ReturnReason =
  | 'DEFECTIVE'
  | 'DAMAGED_IN_TRANSIT'
  | 'WRONG_ITEM'
  | 'NOT_AS_DESCRIBED'
  | 'CHANGED_MIND'
  | 'OVERPURCHASE'
  | 'DUPLICATE_ORDER'
  | 'NO_LONGER_NEEDED'
  | 'RECALL'
  | 'SURPLUS_UNSOLD'
  | 'END_OF_LIFE'
  | 'OTHER';

export type ProductCondition =
  | 'NEW'
  | 'LIKE_NEW'
  | 'GOOD'
  | 'FAIR'
  | 'DAMAGED'
  | 'UNSELLABLE'
  | 'UNSAFE';

export type RecoveryPath =
  | 'RESTOCK_AS_NEW'
  | 'OPEN_BOX_RESALE'
  | 'REPAIR'
  | 'REFURBISHMENT'
  | 'LIQUIDATION'
  | 'SPARE_PARTS'
  | 'RECYCLING'
  | 'SAFE_DISPOSAL';

export type InspectionStatus =
  | 'AWAITING_INSPECTION'
  | 'IN_PROGRESS'
  | 'PASSED'
  | 'DAMAGED'
  | 'INCOMPLETE'
  | 'UNSAFE';

export type JobStatus =
  | 'PROPOSED'
  | 'ASSIGNED'
  | 'ACCEPTED'
  | 'PROCESSING'
  | 'COMPLETED'
  | 'REJECTED'
  | 'CANCELLED'
  | 'FAILED'
  | 'AWAITING_REVIEW';

export type PickupStatus =
  | 'REQUESTED'
  | 'SCHEDULED'
  | 'IN_TRANSIT'
  | 'COLLECTED'
  | 'DELIVERED'
  | 'FAILED'
  | 'CANCELLED';

export type InventoryStatus =
  | 'AVAILABLE'
  | 'RESERVED'
  | 'DAMAGED'
  | 'PROCESSING'
  | 'DISPATCHED'
  | 'COMPLETED'
  | 'QUARANTINED';

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface UserProfile {
  id: string;
  email: string;
  fullName: string;
  role: string;
  jobTitle: string | null;
  organizationId: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserProfile;
}

export interface Product {
  id: string;
  sku: string;
  name: string;
  brand: string | null;
  model: string | null;
  category: string;
  description: string | null;
  imageUrl: string | null;
  weightKg: number | null;
  originalPrice: number | null;
  resaleValue: number | null;
  active: boolean;
}

export interface ReturnItem {
  id: string;
  productId: string;
  sku: string;
  productName: string;
  quantity: number;
  condition: ProductCondition;
  conditionLabel: string;
  serialNumber: string | null;
  accessoriesComplete: boolean;
  estimatedValue: number | null;
  notes: string | null;
}

export interface ReturnEvent {
  id: string;
  eventType: string;
  fromStatus: string | null;
  toStatus: string | null;
  description: string;
  actor: string | null;
  actorRole: string | null;
  occurredAt: string;
}

export interface ReturnSummary {
  id: string;
  returnNumber: string;
  status: ReturnStatus;
  statusLabel: string;
  reason: ReturnReason;
  sourceReference: string | null;
  customerName: string | null;
  itemCount: number;
  estimatedRecoveryValue: number | null;
  realizedRecoveryValue: number | null;
  createdAt: string;
  statusChangedAt: string;
}

export interface ReturnDetail {
  id: string;
  returnNumber: string;
  status: ReturnStatus;
  statusLabel: string;
  reason: ReturnReason;
  reasonLabel: string;
  sourceReference: string | null;
  customerName: string | null;
  customerEmail: string | null;
  originAddress: string | null;
  originLatitude: number | null;
  originLongitude: number | null;
  warehouseId: string | null;
  pickupId: string | null;
  notes: string | null;
  itemCount: number;
  estimatedRecoveryValue: number | null;
  realizedRecoveryValue: number | null;
  cancelledReason: string | null;
  createdAt: string;
  statusChangedAt: string;
  items: ReturnItem[];
  timeline: ReturnEvent[];
}

export interface RecoveryOption {
  path: RecoveryPath;
  label: string;
  expectedResaleValue: number;
  processingCost: number;
  transportationCost: number;
  partnerFees: number;
  inspectionCost: number;
  netRecoveryValue: number;
  estimatedProcessingDays: number;
  dataQualityScore: number;
  explanation: string;
  restrictions: string[];
  eligible: boolean;
  wasteDivertedKg: number;
  carbonAvoidedKg: number;
}

export interface RecoveryItemEvaluation {
  returnItemId: string;
  sku: string;
  productName: string;
  quantity: number;
  condition: ProductCondition;
  recommended: RecoveryOption | null;
  options: RecoveryOption[];
  resaleValue: number;
  potentialValue: number;
}

export interface RecoveryEvaluation {
  returnId: string;
  returnNumber: string;
  strategy: string;
  items: RecoveryItemEvaluation[];
  generatedAt: string;
}

export interface RecoveryRecommendation {
  id: string;
  returnId: string;
  returnItemId: string;
  sku: string;
  recommendedPath: RecoveryPath;
  recommendedPathLabel: string;
  recommendedNetValue: number | null;
  estimatedProcessingDays: number | null;
  strategy: string;
  approved: boolean;
  rejected: boolean;
  decisionReason: string | null;
  decidedBy: string | null;
  decidedAt: string | null;
  options: RecoveryOption[];
  createdAt: string;
}

export interface Inspection {
  id: string;
  returnId: string;
  returnItemId: string;
  sku: string;
  productName: string;
  status: InspectionStatus;
  statusLabel: string;
  packagingCondition: string | null;
  functionalityResult: string | null;
  functional: boolean;
  accessoriesComplete: boolean;
  damageDescription: string | null;
  missingAccessories: string | null;
  estimatedRepairCost: number | null;
  flaggedUnsafe: boolean;
  inspectedBy: string | null;
  inspectedAt: string | null;
  createdAt: string;
}

export interface InventoryItem {
  id: string;
  sku: string;
  productId: string | null;
  productName: string;
  category: string | null;
  quantity: number;
  reservedQuantity: number;
  availableQuantity: number;
  status: InventoryStatus;
  statusLabel: string;
  condition: string;
  warehouseId: string | null;
  locationCode: string | null;
  estimatedValue: number | null;
  unitValue: number | null;
  weightKg: number | null;
  returnId: string | null;
  recoveryJobId: string | null;
  receivedAt: string;
  dispatchedAt: string | null;
  createdAt: string;
}

export interface AgingItem {
  id: string;
  sku: string;
  productName: string;
  quantity: number;
  status: string;
  estimatedValue: number | null;
  receivedAt: string;
  daysHeld: number;
  thresholdDays: number;
}

export interface Partner {
  id: string;
  name: string;
  partnerType: string;
  partnerTypeLabel: string;
  description: string | null;
  contactName: string | null;
  contactEmail: string | null;
  contactPhone: string | null;
  serviceableRegions: string | null;
  supportedCategories: string | null;
  processingCapacityPerDay: number;
  currentlyAvailable: boolean;
  feePerUnit: number | null;
  feePercentage: number | null;
  acceptanceCriteria: string | null;
  latitude: number | null;
  longitude: number | null;
  address: string | null;
  verified: boolean;
  completedJobs: number;
  failedJobs: number;
  averageProcessingDays: number | null;
  acceptanceRate: number | null;
  onTimeRate: number | null;
}

export interface PartnerMatch {
  id: string;
  name: string;
  partnerType: string;
  available: boolean;
  processingCapacityPerDay: number;
  feePerUnit: number | null;
  feePercentage: number | null;
  averageProcessingDays: number | null;
  onTimeRate: number | null;
  acceptanceRate: number | null;
  completedJobs: number;
  verified: boolean;
  matchReason: string;
}

export interface RecoveryJob {
  id: string;
  jobNumber: string;
  returnId: string | null;
  returnItemId: string | null;
  inventoryItemId: string | null;
  sku: string | null;
  productName: string | null;
  quantity: number;
  recoveryPath: RecoveryPath;
  recoveryPathLabel: string;
  status: JobStatus;
  statusLabel: string;
  partnerId: string | null;
  partnerName: string | null;
  expectedRecoveryValue: number | null;
  costEstimate: number | null;
  realizedValue: number | null;
  serviceDeadline: string | null;
  assignedAt: string | null;
  acceptedAt: string | null;
  startedAt: string | null;
  completedAt: string | null;
  evidenceNote: string | null;
  exceptionNote: string | null;
  rejectionReason: string | null;
  progressNote: string | null;
  createdAt: string;
}

export interface PickupJob {
  id: string;
  pickupNumber: string;
  returnId: string | null;
  status: PickupStatus;
  statusLabel: string;
  originAddress: string | null;
  originLatitude: number | null;
  originLongitude: number | null;
  destinationLocationId: string | null;
  destinationName: string | null;
  scheduledDate: string | null;
  windowStart: string | null;
  windowEnd: string | null;
  priority: number;
  unitCount: number;
  totalWeightKg: number | null;
  vehicleId: string | null;
  driverId: string | null;
  routeId: string | null;
  stopSequence: number | null;
  estimatedDistanceKm: number | null;
  estimatedDurationMinutes: number | null;
  estimatedCost: number | null;
  completedAt: string | null;
  failureReason: string | null;
  evidenceNote: string | null;
  notes: string | null;
  createdAt: string;
}

export interface RouteStop {
  id: string;
  pickupJobId: string | null;
  sequence: number;
  stopType: string;
  locationName: string;
  address: string | null;
  latitude: number | null;
  longitude: number | null;
  unitCount: number;
  legDistanceKm: number | null;
  legDurationMinutes: number | null;
  completed: boolean;
  completedAt: string | null;
  note: string | null;
}

export interface Route {
  id: string;
  routeNumber: string;
  status: string;
  statusLabel: string;
  routeDate: string | null;
  vehicleId: string | null;
  vehicleName: string | null;
  driverId: string | null;
  driverName: string | null;
  originLocationId?: string | null;
  originName: string | null;
  stopCount: number;
  totalUnits: number;
  totalWeightKg: number | null;
  estimatedDistanceKm: number | null;
  estimatedDurationMinutes: number | null;
  estimatedCost: number | null;
  algorithm: string;
  notes: string | null;
  estimatedDistances?: boolean;
  stops?: RouteStop[];
}

export interface KpiCard {
  key: string;
  label: string;
  value: number;
  previousValue: number | null;
  description: string;
}

export interface TrendPoint {
  date: string;
  returnsCreated: number;
  itemsRecovered: number;
}

export interface DistributionSlice {
  path: string;
  label: string;
  count: number;
  percentage: number;
  value: number;
  valueShare: number;
}

export interface MapMarker {
  id: string;
  type: string;
  name: string;
  latitude: number;
  longitude: number;
  status: string;
  statusLabel: string;
  scheduledDate: string | null;
  reference: string;
}

export interface DashboardSummary {
  from: string;
  to: string;
  totalReturns: KpiCard;
  awaitingInspection: KpiCard;
  recoveredValue: KpiCard;
  wasteAvoided: KpiCard;
  averageTurnaround: KpiCard;
  trend: TrendPoint[];
  distribution: DistributionSlice[];
  markers: MapMarker[];
  recentReturns: {
    id: string;
    returnNumber: string;
    status: ReturnStatus;
    statusLabel: string;
    customerName: string | null;
    estimatedRecoveryValue: number | null;
    createdAt: string;
  }[];
}

export interface EnvironmentalSummary {
  wasteDivertedKg: number;
  carbonAvoidedKg: number;
  itemsAssessed: number;
  methodology: string[];
}

export interface PartnerPerformance {
  partnerName: string;
  completedJobs: number;
  onTimeRate: number | null;
  averageProcessingDays: number | null;
}

export interface ReportSummary {
  from: string;
  to: string;
  totalReturns: number;
  recoverySuccessRate: number;
  estimatedValue: number;
  realizedValue: number;
  realizationRate: number;
  averageTurnaroundDays: number | null;
  recoveryDistribution: DistributionSlice[];
  partnerPerformance: PartnerPerformance[];
  consolidatedPickups: number;
  wasteAvoidedValue: number;
  environmental: EnvironmentalSummary;
}

export interface Notification {
  id: string;
  title: string;
  body: string | null;
  category: string;
  severity: string;
  link: string | null;
  referenceType: string | null;
  referenceId: string | null;
  read: boolean;
  createdAt: string;
}

export interface AuditEvent {
  id: string;
  actor: string;
  actorRole: string | null;
  action: string;
  entityType: string;
  entityId: string | null;
  summary: string | null;
  ipAddress: string | null;
  occurredAt: string;
}

export interface Location {
  id: string;
  name: string;
  locationType: string;
  locationTypeLabel: string;
  address: string | null;
  latitude: number;
  longitude: number;
  region: string | null;
  capacityUnits: number | null;
  active: boolean;
}

export interface Vehicle {
  id: string;
  name: string;
  registration: string;
  vehicleType: string;
  capacityUnits: number;
  capacityKg: number | null;
  homeLocationId?: string | null;
  active: boolean;
  notes: string | null;
}

export interface Driver {
  id: string;
  name: string;
  phone: string | null;
  email: string | null;
  licenseNumber: string | null;
  active: boolean;
}

export interface Organization {
  id: string;
  name: string;
  slug: string;
  contactEmail: string | null;
  country: string | null;
  defaultCurrency: string;
  active: boolean;
  holdingThresholdDays: number;
}

export interface ImportReport {
  type: string;
  rowsRead: number;
  rowsImported: number;
  importedReferences: string[];
  errors: { line: number; returnNumber: string | null; message: string }[];
  notes: string;
}
