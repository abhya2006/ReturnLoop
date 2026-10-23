import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ShieldCheck, UserPlus, Users } from 'lucide-react';
import { usersApi } from '@/api/client';
import { Card, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { EmptyState } from '@/components/ui/EmptyState';
import { Field, Input, Select } from '@/components/ui/Form';
import { Modal } from '@/components/ui/Modal';
import { SkeletonTable } from '@/components/ui/Skeleton';
import { StatusPill } from '@/components/ui/StatusPill';
import { useToast } from '@/components/ui/Toast';
import { ApiError } from '@/lib/api';
import { formatDateTime } from '@/lib/format';

const ROLES = [
  'ADMIN',
  'OPERATIONS_MANAGER',
  'WAREHOUSE_INSPECTOR',
  'LOGISTICS_PLANNER',
  'PARTNER',
  'ANALYST',
  'VIEWER',
];

export function UsersPage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [createOpen, setCreateOpen] = useState(false);

  const query = useQuery({
    queryKey: ['users'],
    queryFn: () => usersApi.list(),
  });

  const create = useMutation({
    mutationFn: (body: {
      email: string;
      fullName: string;
      password: string;
      role: string;
      jobTitle?: string;
    }) => usersApi.create(body),
    onSuccess: () => {
      toast.success('User created');
      setCreateOpen(false);
      void queryClient.invalidateQueries({ queryKey: ['users'] });
    },
    onError: (error) =>
      toast.error('The user could not be created', error instanceof ApiError ? error.message : undefined),
  });

  const users = query.data ?? [];

  return (
    <div className="space-y-4">
      <Card>
        <div className="flex flex-wrap items-center justify-between gap-2 border-b border-edge px-4 py-3">
          <p className="text-xs text-ink-muted">
            Everyone with access to this organisation, and the role that governs what they can do.
          </p>
          <Button size="sm" onClick={() => setCreateOpen(true)} icon={<UserPlus className="h-3.5 w-3.5" />}>
            Add user
          </Button>
        </div>

        {query.isLoading ? (
          <SkeletonTable rows={6} columns={5} />
        ) : users.length === 0 ? (
          <EmptyState
            icon={<Users className="h-5 w-5" />}
            title="No users yet"
            description="Invite your team to the workspace."
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="data-table rows-enter">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Role</th>
                  <th>Job title</th>
                  <th>Status</th>
                  <th>Last sign-in</th>
                </tr>
              </thead>
              <tbody>
                {users.map((user: {
                  id: string;
                  fullName: string;
                  email: string;
                  role: string;
                  jobTitle: string | null;
                  active: boolean;
                  lastLoginAt: string | null;
                }) => (
                  <tr key={user.id}>
                    <td className="font-medium text-ink-primary">{user.fullName}</td>
                    <td className="text-ink-secondary">{user.email}</td>
                    <td>
                      <StatusPill
                        label={user.role.replace(/_/g, ' ').toLowerCase()}
                        tone={user.role === 'ADMIN' ? 'accent' : 'info'}
                      />
                    </td>
                    <td className="text-ink-muted">{user.jobTitle ?? '—'}</td>
                    <td>
                      <StatusPill
                        label={user.active ? 'Active' : 'Deactivated'}
                        tone={user.active ? 'success' : 'danger'}
                        dot
                      />
                    </td>
                    <td className="whitespace-nowrap text-ink-muted">
                      {user.lastLoginAt ? formatDateTime(user.lastLoginAt) : 'Never'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <Card className="reveal">
        <CardHeader
          title="What each role can do"
          description="Role-based access control is enforced on the server, not just hidden in the interface"
        />
        <div className="grid grid-cols-1 gap-3 px-5 py-4 md:grid-cols-2 xl:grid-cols-3">
          {[
            ['ADMIN', 'Everything, including users, settings and audit history.'],
            ['OPERATIONS_MANAGER', 'Run the operation: returns, recovery decisions and partner assignment.'],
            ['WAREHOUSE_INSPECTOR', 'Open and complete warehouse inspections.'],
            ['LOGISTICS_PLANNER', 'Schedule pickups, assign vehicles and optimise routes.'],
            ['PARTNER', 'Accept and progress jobs assigned to their organisation.'],
            ['ANALYST', 'Read-only access to reports and environmental impact.'],
            ['VIEWER', 'Read-only access to operational records.'],
          ].map(([role, description]) => (
            <div key={role} className="rounded-lg border border-edge bg-surface-base px-3.5 py-3">
              <p className="flex items-center gap-1.5 text-[12px] font-semibold text-ink-primary">
                <ShieldCheck className="h-3.5 w-3.5 text-cerulean-light" aria-hidden />
                {role.replace(/_/g, ' ')}
              </p>
              <p className="mt-1 text-[11px] leading-relaxed text-ink-muted">{description}</p>
            </div>
          ))}
        </div>
      </Card>

      <CreateUserModal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onSubmit={(body) => create.mutate(body)}
        pending={create.isPending}
      />
    </div>
  );
}

function CreateUserModal({
  open,
  onClose,
  onSubmit,
  pending,
}: {
  open: boolean;
  onClose: () => void;
  onSubmit: (body: {
    email: string;
    fullName: string;
    password: string;
    role: string;
    jobTitle?: string;
  }) => void;
  pending: boolean;
}) {
  const [form, setForm] = useState({
    email: '',
    fullName: '',
    password: '',
    role: 'VIEWER',
    jobTitle: '',
  });

  const update = (key: keyof typeof form, value: string) =>
    setForm((current) => ({ ...current, [key]: value }));

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Add a user"
      description="The user joins this organisation with the role you choose."
      size="sm"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button
            loading={pending}
            disabled={!form.email || !form.fullName || form.password.length < 10}
            onClick={() => onSubmit(form)}
          >
            Create user
          </Button>
        </>
      }
    >
      <div className="space-y-3">
        <Field label="Full name" required>
          <Input value={form.fullName} onChange={(event) => update('fullName', event.target.value)} />
        </Field>
        <Field label="Work email" required>
          <Input
            type="email"
            value={form.email}
            onChange={(event) => update('email', event.target.value)}
          />
        </Field>
        <Field label="Role" required>
          <Select value={form.role} onChange={(event) => update('role', event.target.value)}>
            {ROLES.map((role) => (
              <option key={role} value={role}>
                {role.replace(/_/g, ' ').toLowerCase()}
              </option>
            ))}
          </Select>
        </Field>
        <Field label="Job title">
          <Input value={form.jobTitle} onChange={(event) => update('jobTitle', event.target.value)} />
        </Field>
        <Field label="Temporary password" hint="At least 10 characters" required>
          <Input
            type="password"
            value={form.password}
            onChange={(event) => update('password', event.target.value)}
          />
        </Field>
      </div>
    </Modal>
  );
}
