import {
  BarChart3,
  Bell,
  Boxes,
  Building2,
  ClipboardCheck,
  FileSpreadsheet,
  Leaf,
  MapPin,
  Package,
  Recycle,
  Route,
  ScrollText,
  Settings,
  ShieldCheck,
  Truck,
  Users,
  type LucideIcon,
} from 'lucide-react';

export interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
  description: string;
  /** Marks items that carry a badge count. */
  badge?: 'notifications';
}

export interface NavGroup {
  title: string;
  items: NavItem[];
}

export const navigation: NavGroup[] = [
  {
    title: 'Overview',
    items: [
      {
        to: '/',
        label: 'Dashboard',
        icon: BarChart3,
        description: 'Network health, trends and recovery mix',
      },
      {
        to: '/reports',
        label: 'Reports & Analytics',
        icon: Recycle,
        description: 'Recovery economics and partner performance',
      },
      {
        to: '/environmental',
        label: 'Environmental Impact',
        icon: Leaf,
        description: 'Estimated waste diversion and carbon avoided',
      },
    ],
  },
  {
    title: 'Operations',
    items: [
      {
        to: '/returns',
        label: 'Returns Management',
        icon: Package,
        description: 'Create, track and progress return requests',
      },
      {
        to: '/imports',
        label: 'Bulk Import (CSV)',
        icon: FileSpreadsheet,
        description: 'Ingest returns and unsold inventory in bulk',
      },
      {
        to: '/inventory',
        label: 'Inventory Tracking',
        icon: Boxes,
        description: 'Stock on hand, movements and holding-time risk',
      },
      {
        to: '/inspections',
        label: 'Inspections',
        icon: ClipboardCheck,
        description: 'Warehouse inspection queue and findings',
      },
      {
        to: '/recovery',
        label: 'Recovery Engine',
        icon: ShieldCheck,
        description: 'Compare recovery paths and approve decisions',
      },
    ],
  },
  {
    title: 'Logistics',
    items: [
      {
        to: '/pickups',
        label: 'Pickup Scheduling',
        icon: Truck,
        description: 'Collection windows, vehicles and drivers',
      },
      {
        to: '/routes',
        label: 'Route Optimization',
        icon: Route,
        description: 'Consolidate pickups into capacity-aware routes',
      },
    ],
  },
  {
    title: 'Partners',
    items: [
      {
        to: '/partners',
        label: 'Partner Marketplace',
        icon: Building2,
        description: 'Repair, refurbishment, resale and recycling partners',
      },
      {
        to: '/partner-jobs',
        label: 'Partner Jobs',
        icon: MapPin,
        description: 'Recovery job lifecycle and outcomes',
      },
    ],
  },
  {
    title: 'Administration',
    items: [
      {
        to: '/notifications',
        label: 'Notifications',
        icon: Bell,
        description: 'Events requiring your attention',
        badge: 'notifications',
      },
      {
        to: '/users',
        label: 'Users & Roles',
        icon: Users,
        description: 'Team access and role assignment',
      },
      {
        to: '/organizations',
        label: 'Organizations',
        icon: Building2,
        description: 'Tenant settings and holding thresholds',
      },
      {
        to: '/audit',
        label: 'Audit Logs',
        icon: ScrollText,
        description: 'Trace every privileged action',
      },
      {
        to: '/settings',
        label: 'Settings',
        icon: Settings,
        description: 'Workspace preferences and recovery configuration',
      },
    ],
  },
];

export const flatNavigation = navigation.flatMap((group) => group.items);
