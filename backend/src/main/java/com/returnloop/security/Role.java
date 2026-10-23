package com.returnloop.security;

/**
 * Application roles. Permissions are derived from these via
 * {@link org.springframework.security.access.prepost.PostAuthorize} expressions
 * and method level checks in the service layer.
 */
public enum Role {

    /** Full access to the organisation, including user management and settings. */
    ADMIN,

    /** Runs the day-to-day reverse logistics operation. */
    OPERATIONS_MANAGER,

    /** Performs warehouse inspections. */
    WAREHOUSE_INSPECTOR,

    /** Plans pickups, assigns vehicles and optimises routes. */
    LOGISTICS_PLANNER,

    /** External partner account; can only see and act on jobs assigned to it. */
    PARTNER,

    /** Read-only access to reporting and analytics. */
    ANALYST,

    /** Read-only access to operational records. */
    VIEWER
}
