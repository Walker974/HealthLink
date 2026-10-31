export interface paths {
    "/api/work-orders": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * List work orders for equipment
         * @description Scoped staff access. Newest reports first. Page is zero-based; size must be 1–100.
         */
        get: operations["list"];
        put?: never;
        /**
         * Report a fault and open a work order
         * @description RADIOLOGY_OPERATOR, own department. Submit the current equipmentVersion from GET /api/equipment/{id}. Only one open work order is allowed per machine; stale versions and duplicates return 409.
         */
        post: operations["report"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/work-orders/{id}/verify": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Verify a repair and restore availability
         * @description HOSPITAL_MANAGER, own hospital. The manager must be different from the repairer, even after a role change. Moves AWAITING_VERIFICATION to VERIFIED and equipment to AVAILABLE.
         */
        post: operations["verify"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/work-orders/{id}/reject": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Reject a repair
         * @description HOSPITAL_MANAGER, own hospital, different from the repairer. A reason is required. Returns the work order to IN_PROGRESS for the same technician; equipment remains unavailable.
         */
        post: operations["reject"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/work-orders/{id}/complete": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Submit a completed repair for verification
         * @description Only the assigned technician. Moves IN_PROGRESS to AWAITING_VERIFICATION; equipment remains unavailable.
         */
        post: operations["complete"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/work-orders/{id}/accept": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Accept a repair
         * @description MAINTENANCE_TECHNICIAN, own hospital. Assigns the authenticated technician and moves OPEN to IN_PROGRESS. Equipment enters UNDER_MAINTENANCE.
         */
        post: operations["accept"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/staff": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Link a Keycloak user to a staff profile
         * @description Requires PLATFORM_ADMIN. The user must be created and assigned the matching realm role in Keycloak separately. This endpoint stores the immutable Keycloak subject and organizational assignment; it does not create Keycloak accounts, set passwords or grant realm roles. Hospital roles require a hospital. Only RADIOLOGY_OPERATOR requires a department, which must belong to that hospital. PLATFORM_ADMIN and MINISTRY_OFFICER have neither assignment.
         */
        post: operations["create"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/organization/regions": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * List regions
         * @description Public reference data, ordered by ID.
         */
        get: operations["listRegions"];
        put?: never;
        /**
         * Create a region
         * @description Requires PLATFORM_ADMIN.
         */
        post: operations["createRegion"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/organization/hospitals": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Create a hospital
         * @description Requires PLATFORM_ADMIN and an existing region.
         */
        post: operations["createHospital"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/organization/departments": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Create a department
         * @description Requires HOSPITAL_MANAGER with an active staff assignment to the requested hospital.
         */
        post: operations["createDepartment"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/equipment": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Register equipment
         * @description Requires HOSPITAL_MANAGER with an active staff assignment to the department's hospital. The department must be active and the imaging type must already exist. New equipment starts as AVAILABLE.
         */
        post: operations["register"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/equipment/{id}/status": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        /**
         * Report a fault through the compatibility status endpoint
         * @deprecated
         * @description Requires RADIOLOGY_OPERATOR with an active staff assignment to the equipment's department and hospital. Requires the current equipment version and a fault description. Creates a work order atomically; duplicates and stale versions return 409. Prefer POST /api/work-orders. Only OUT_OF_SERVICE is accepted.
         */
        patch: operations["changeStatus"];
        trace?: never;
    };
    "/api/work-orders/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get a work order
         * @description Hospital staff in their own hospital; operators are restricted to their department.
         */
        get: operations["get"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/staff/me": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get your staff profile
         * @description Requires a valid Keycloak token and an active local staff profile with a matching role. An unlinked platform administrator can provision organizations and staff, but receives 403 here until a local profile is linked.
         */
        get: operations["me"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/organization/regions/{regionId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get a region
         * @description Public reference data; no authentication required.
         */
        get: operations["getRegion"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/organization/regions/{regionId}/hospitals": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * List hospitals in a region
         * @description Public reference data, ordered by ID.
         */
        get: operations["listHospitals"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/organization/hospitals/{hospitalId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get a hospital
         * @description Public reference data; no authentication required.
         */
        get: operations["getHospital"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/organization/hospitals/{hospitalId}/departments": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * List departments in a hospital
         * @description Requires HOSPITAL_MANAGER, RADIOLOGY_OPERATOR or MAINTENANCE_TECHNICIAN with an active staff assignment to the requested hospital. Results are ordered by ID.
         */
        get: operations["listDepartments"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/organization/departments/{departmentId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get a department
         * @description Requires HOSPITAL_MANAGER, RADIOLOGY_OPERATOR or MAINTENANCE_TECHNICIAN with an active staff assignment to the department's hospital.
         */
        get: operations["getDepartment"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/equipment/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get equipment details and its current version
         * @description Hospital staff in their own hospital; operators are restricted to their department.
         */
        get: operations["get_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/equipment/{equipmentId}/history": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Read equipment and repair history
         * @description Hospital manager or technician in the same hospital, or operator in the equipment's department. Records are ordered oldest first. Page is zero-based; size must be 1–100. History remains readable after equipment deactivation.
         */
        get: operations["history"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/equipment/available": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Find available imaging equipment
         * @description Public citizen search by imaging type code and city. No account or token is required. Returns active equipment whose status is AVAILABLE; an empty list means no matches.
         */
        get: operations["findAvailable"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
}
export type webhooks = Record<string, never>;
export interface components {
    schemas: {
        ReportFaultRequest: {
            /**
             * Format: int64
             * @example 1
             */
            equipmentId: number;
            /**
             * Format: int64
             * @description Current equipment version from GET /api/equipment/{id}
             * @example 0
             */
            equipmentVersion: number;
            /** @example Scanner fails its startup self-test */
            description: string;
        };
        WorkOrderResponse: {
            /** Format: int64 */
            id?: number;
            /** Format: int64 */
            version?: number;
            /** Format: int64 */
            equipmentId?: number;
            /** Format: int64 */
            equipmentVersion?: number;
            /** @enum {string} */
            equipmentStatus?: "AVAILABLE" | "OUT_OF_SERVICE" | "UNDER_MAINTENANCE";
            /** Format: int64 */
            hospitalId?: number;
            /** Format: int64 */
            departmentId?: number;
            /** @enum {string} */
            status?: "OPEN" | "IN_PROGRESS" | "AWAITING_VERIFICATION" | "VERIFIED";
            description?: string;
            repairNotes?: string;
            verificationNotes?: string;
            /** Format: int64 */
            reporterId?: number;
            /** Format: int64 */
            repairerId?: number;
            /** Format: int64 */
            verifierId?: number;
            /** Format: date-time */
            reportedAt?: string;
            /** Format: date-time */
            acceptedAt?: string;
            /** Format: date-time */
            completedAt?: string;
            /** Format: date-time */
            reviewedAt?: string;
            /** Format: date-time */
            verifiedAt?: string;
        };
        ReviewRepairRequest: {
            /** Format: int64 */
            version: number;
            /**
             * @description Verification findings, or the reason for rejection
             * @example Independent operational checks passed
             */
            notes: string;
        };
        CompleteRepairRequest: {
            /** Format: int64 */
            version: number;
            /** @example Replaced faulty power supply; startup test passes */
            repairNotes: string;
        };
        VersionRequest: {
            /**
             * Format: int64
             * @description Current work-order version
             * @example 0
             */
            version: number;
        };
        CreateStaffRequest: {
            /**
             * @description Immutable Keycloak user ID (JWT sub), not the username or email
             * @example 550e8400-e29b-41d4-a716-446655440000
             */
            keycloakSubject: string;
            /** @example Demo Manager */
            fullName: string;
            /**
             * Format: email
             * @example manager@example.test
             */
            email: string;
            /**
             * @description Must match the user's separately assigned Keycloak realm role
             * @example HOSPITAL_MANAGER
             * @enum {string}
             */
            role: "RADIOLOGY_OPERATOR" | "MAINTENANCE_TECHNICIAN" | "HOSPITAL_MANAGER" | "MINISTRY_OFFICER" | "PLATFORM_ADMIN";
            /**
             * Format: int64
             * @description Required for manager, operator and technician; omit for admin and ministry roles
             * @example 1
             */
            hospitalId?: number;
            /**
             * Format: int64
             * @description Required only for operators; must belong to the assigned hospital. Omit for other roles.
             */
            departmentId?: number;
        };
        StaffResponse: {
            /** Format: int64 */
            id?: number;
            keycloakSubject?: string;
            fullName?: string;
            email?: string;
            /** @enum {string} */
            role?: "RADIOLOGY_OPERATOR" | "MAINTENANCE_TECHNICIAN" | "HOSPITAL_MANAGER" | "MINISTRY_OFFICER" | "PLATFORM_ADMIN";
            /** Format: int64 */
            hospitalId?: number;
            /** Format: int64 */
            departmentId?: number;
        };
        CreateRegionRequest: {
            /**
             * @description Unique region code
             * @example FES_MEKNES
             */
            code: string;
            /** @example Fes-Meknes */
            name: string;
        };
        RegionResponse: {
            /** Format: int64 */
            id?: number;
            code?: string;
            name?: string;
        };
        CreateHospitalRequest: {
            /**
             * @description Unique hospital code
             * @example DEMO_FES
             */
            code: string;
            /** @example Demo Hospital */
            name: string;
            /** @example Fes */
            city: string;
            /** @example Demo address */
            address: string;
            /**
             * Format: int64
             * @description ID of an existing region
             * @example 1
             */
            regionId: number;
        };
        HospitalResponse: {
            /** Format: int64 */
            id?: number;
            code?: string;
            name?: string;
            city?: string;
            address?: string;
            /** Format: int64 */
            regionId?: number;
        };
        CreateDepartmentRequest: {
            /**
             * @description Code unique within the hospital
             * @example RADIOLOGY
             */
            code: string;
            /** @example Radiology */
            name: string;
            /**
             * Format: int64
             * @description ID of the manager's assigned hospital
             * @example 1
             */
            hospitalId: number;
        };
        DepartmentResponse: {
            /** Format: int64 */
            id?: number;
            code?: string;
            name?: string;
            /** Format: int64 */
            hospitalId?: number;
        };
        RegisterEquipmentRequest: {
            /**
             * @description Unique equipment serial number
             * @example DEMO-MRI-001
             */
            serialNumber: string;
            /** @example Demo MRI model */
            model: string;
            /**
             * Format: int64
             * @description Active department in the manager's assigned hospital
             * @example 1
             */
            departmentId: number;
            /**
             * @description Code of an existing imaging type
             * @example MRI
             */
            imagingTypeCode: string;
        };
        EquipmentResponse: {
            /** Format: int64 */
            id?: number;
            /** Format: int64 */
            version?: number;
            serialNumber?: string;
            model?: string;
            /** @enum {string} */
            status?: "AVAILABLE" | "OUT_OF_SERVICE" | "UNDER_MAINTENANCE";
            /** Format: date-time */
            statusUpdatedAt?: string;
            /** Format: int64 */
            departmentId?: number;
            imagingTypeCode?: string;
            /** Format: int64 */
            hospitalId?: number;
            hospitalName?: string;
            city?: string;
            address?: string;
        };
        ChangeStatusRequest: {
            /**
             * @description Only OUT_OF_SERVICE is accepted by the fault-reporting endpoint. This compatibility endpoint creates a work order. Prefer POST /api/work-orders.
             * @example OUT_OF_SERVICE
             * @enum {string}
             */
            status: "OUT_OF_SERVICE";
            /**
             * Format: int64
             * @description Current equipment version
             * @example 0
             */
            version: number;
            /** @example Scanner fails its startup self-test */
            description: string;
        };
        PageResponseRegionResponse: {
            items?: components["schemas"]["RegionResponse"][];
            /**
             * Format: int32
             * @description Zero-based page index
             * @example 0
             */
            page?: number;
            /**
             * Format: int32
             * @description Requested page size
             * @example 20
             */
            size?: number;
            /**
             * Format: int64
             * @description Total number of matching records
             * @example 1
             */
            totalElements?: number;
            /**
             * Format: int32
             * @description Total number of pages
             * @example 1
             */
            totalPages?: number;
        };
        PageResponseHospitalResponse: {
            items?: components["schemas"]["HospitalResponse"][];
            /**
             * Format: int32
             * @description Zero-based page index
             * @example 0
             */
            page?: number;
            /**
             * Format: int32
             * @description Requested page size
             * @example 20
             */
            size?: number;
            /**
             * Format: int64
             * @description Total number of matching records
             * @example 1
             */
            totalElements?: number;
            /**
             * Format: int32
             * @description Total number of pages
             * @example 1
             */
            totalPages?: number;
        };
        PageResponseDepartmentResponse: {
            items?: components["schemas"]["DepartmentResponse"][];
            /**
             * Format: int32
             * @description Zero-based page index
             * @example 0
             */
            page?: number;
            /**
             * Format: int32
             * @description Requested page size
             * @example 20
             */
            size?: number;
            /**
             * Format: int64
             * @description Total number of matching records
             * @example 1
             */
            totalElements?: number;
            /**
             * Format: int32
             * @description Total number of pages
             * @example 1
             */
            totalPages?: number;
        };
        AuditResponse: {
            /** Format: uuid */
            id?: string;
            /** Format: date-time */
            occurredAt?: string;
            /** @enum {string} */
            action?: "EQUIPMENT_REGISTERED" | "FAULT_REPORTED" | "REPAIR_ACCEPTED" | "REPAIR_COMPLETED" | "REPAIR_VERIFIED" | "REPAIR_REJECTED";
            /** Format: int64 */
            equipmentId?: number;
            /** Format: int64 */
            hospitalId?: number;
            /** Format: int64 */
            workOrderId?: number;
            actorSubject?: string;
            /** Format: int64 */
            actorStaffId?: number;
            actorRole?: string;
            /** @enum {string} */
            previousEquipmentStatus?: "AVAILABLE" | "OUT_OF_SERVICE" | "UNDER_MAINTENANCE";
            /** @enum {string} */
            equipmentStatus?: "AVAILABLE" | "OUT_OF_SERVICE" | "UNDER_MAINTENANCE";
            previousWorkOrderStatus?: string;
            workOrderStatus?: string;
            details?: string;
        };
        PageResponseAuditResponse: {
            items?: components["schemas"]["AuditResponse"][];
            /** Format: int32 */
            page?: number;
            /** Format: int32 */
            size?: number;
            /** Format: int64 */
            totalElements?: number;
            /** Format: int32 */
            totalPages?: number;
        };
    };
    responses: {
        /** @description Invalid request body, parameters, pagination or staff assignment */
        BadRequest: {
            headers: {
                [name: string]: unknown;
            };
            content?: never;
        };
        /** @description Missing, expired or invalid bearer access token */
        Unauthorized: {
            headers: {
                [name: string]: unknown;
            };
            content?: never;
        };
        /** @description Insufficient role or missing, inactive or mismatched staff assignment */
        Forbidden: {
            headers: {
                [name: string]: unknown;
            };
            content?: never;
        };
        /** @description A referenced resource does not exist or is inactive where required */
        NotFound: {
            headers: {
                [name: string]: unknown;
            };
            content?: never;
        };
        /** @description Stale/concurrent update, invalid workflow transition, duplicate open repair, or uniqueness/database constraint conflict */
        Conflict: {
            headers: {
                [name: string]: unknown;
            };
            content?: never;
        };
    };
    parameters: never;
    requestBodies: never;
    headers: never;
    pathItems: never;
}
export type $defs = Record<string, never>;
export interface operations {
    list: {
        parameters: {
            query: {
                equipmentId: number;
                page?: number;
                size?: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
        };
    };
    report: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ReportFaultRequest"];
            };
        };
        responses: {
            /** @description Fault, unavailable equipment and audit entry committed together */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["WorkOrderResponse"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
        };
    };
    verify: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ReviewRepairRequest"];
            };
        };
        responses: {
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
        };
    };
    reject: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ReviewRepairRequest"];
            };
        };
        responses: {
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
        };
    };
    complete: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CompleteRepairRequest"];
            };
        };
        responses: {
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
        };
    };
    accept: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["VersionRequest"];
            };
        };
        responses: {
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
        };
    };
    create: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateStaffRequest"];
            };
        };
        responses: {
            /** @description Staff profile linked */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["StaffResponse"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
        };
    };
    listRegions: {
        parameters: {
            query?: {
                /** @description Zero-based page index */
                page?: string;
                /** @description Number of records per page */
                size?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Page of regions */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PageResponseRegionResponse"];
                };
            };
            400: components["responses"]["BadRequest"];
        };
    };
    createRegion: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateRegionRequest"];
            };
        };
        responses: {
            /** @description Region created */
            201: {
                headers: {
                    /** @description URL of the created region */
                    Location?: string;
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RegionResponse"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            409: components["responses"]["Conflict"];
        };
    };
    createHospital: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateHospitalRequest"];
            };
        };
        responses: {
            /** @description Hospital created */
            201: {
                headers: {
                    /** @description URL of the created hospital */
                    Location?: string;
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["HospitalResponse"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
        };
    };
    createDepartment: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateDepartmentRequest"];
            };
        };
        responses: {
            /** @description Department created */
            201: {
                headers: {
                    /** @description URL of the created department */
                    Location?: string;
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["DepartmentResponse"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
        };
    };
    register: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["RegisterEquipmentRequest"];
            };
        };
        responses: {
            /** @description Equipment registered */
            201: {
                headers: {
                    /** @description URL of the registered equipment */
                    Location?: string;
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["EquipmentResponse"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
        };
    };
    changeStatus: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ChangeStatusRequest"];
            };
        };
        responses: {
            /** @description Equipment is out of service and a work order was opened */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["EquipmentResponse"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
        };
    };
    get: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
        };
    };
    me: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Current user's staff profile */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["StaffResponse"];
                };
            };
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
        };
    };
    getRegion: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                regionId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Region details */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RegionResponse"];
                };
            };
            400: components["responses"]["BadRequest"];
            404: components["responses"]["NotFound"];
        };
    };
    listHospitals: {
        parameters: {
            query?: {
                /** @description Zero-based page index */
                page?: string;
                /** @description Number of records per page */
                size?: string;
            };
            header?: never;
            path: {
                regionId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Page of hospitals */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PageResponseHospitalResponse"];
                };
            };
            400: components["responses"]["BadRequest"];
            404: components["responses"]["NotFound"];
        };
    };
    getHospital: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                hospitalId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Hospital details */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["HospitalResponse"];
                };
            };
            400: components["responses"]["BadRequest"];
            404: components["responses"]["NotFound"];
        };
    };
    listDepartments: {
        parameters: {
            query?: {
                /** @description Zero-based page index */
                page?: string;
                /** @description Number of records per page */
                size?: string;
            };
            header?: never;
            path: {
                hospitalId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Page of departments */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PageResponseDepartmentResponse"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
        };
    };
    getDepartment: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                departmentId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Department details */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["DepartmentResponse"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
        };
    };
    get_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Equipment details including optimistic-lock version */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["EquipmentResponse"];
                };
            };
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
        };
    };
    history: {
        parameters: {
            query?: {
                page?: number;
                size?: number;
            };
            header?: never;
            path: {
                equipmentId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Page of immutable audit records */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PageResponseAuditResponse"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
        };
    };
    findAvailable: {
        parameters: {
            query: {
                /**
                 * @description Existing imaging type code
                 * @example MRI
                 */
                type: string;
                /**
                 * @description City used to filter hospitals
                 * @example Fes
                 */
                city: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Matching available equipment */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["EquipmentResponse"][];
                };
            };
            400: components["responses"]["BadRequest"];
        };
    };
}
