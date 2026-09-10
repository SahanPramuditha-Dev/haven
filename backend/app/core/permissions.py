from enum import Enum
from typing import Optional, Dict, Any

class MemberRole(str, Enum):
    OWNER = "OWNER"
    FAMILY_ADMIN = "FAMILY_ADMIN"
    PARENT_GUARDIAN = "PARENT_GUARDIAN"
    ADULT = "ADULT"
    TEEN = "TEEN"
    CHILD = "CHILD"
    MEMBER = "MEMBER"
    GUEST = "GUEST"

class ResourceType(str, Enum):
    FAMILY = "FAMILY"
    HOUSEHOLD = "HOUSEHOLD"
    MEMBER = "MEMBER"
    MESSAGE = "MESSAGE"
    EVENT = "EVENT"
    TASK = "TASK"
    SAFETY = "SAFETY"
    FINANCE = "FINANCE"
    VAULT = "VAULT"
    KIDS = "KIDS"
    HEALTH = "HEALTH"
    GALLERY = "GALLERY"
    VEHICLE = "VEHICLE"
    PET = "PET"
    EDUCATION = "EDUCATION"
    TRIP = "TRIP"
    MEMORY = "MEMORY"
    NOTIFICATION = "NOTIFICATION"
    AUDIT = "AUDIT"

class ActionType(str, Enum):
    CREATE = "CREATE"
    READ = "READ"
    UPDATE = "UPDATE"
    DELETE = "DELETE"
    APPROVE = "APPROVE"
    MANAGE_ROLES = "MANAGE_ROLES"
    EXPORT = "EXPORT"

def is_authorized(
    role: MemberRole,
    resource: ResourceType,
    action: ActionType,
    is_owner: bool = False,
    custom_permissions: Optional[Dict[str, Any]] = None
) -> bool:
    # 1. Custom permission overrides from family_members.permissions JSON
    if custom_permissions:
        resource_rules = custom_permissions.get("modules", {}).get(resource.value.lower())
        if resource_rules is not None and isinstance(resource_rules, dict):
            action_key = action.value.lower()
            if action_key in resource_rules:
                return bool(resource_rules[action_key])

    # 2. Owner and Family Admin have full access across modules
    if role in (MemberRole.OWNER, MemberRole.FAMILY_ADMIN):
        return True

    # 3. Secure Vault & Health data: strict boundary
    if resource in (ResourceType.VAULT, ResourceType.HEALTH):
        if role in (MemberRole.PARENT_GUARDIAN, MemberRole.ADULT):
            # Parents/Adults can access unless explicitly restricted
            return True if action == ActionType.READ else is_owner
        return False

    # 4. Finance & Budgets: restricted for Children / Teens / Guests
    if resource == ResourceType.FINANCE:
        if role in (MemberRole.CHILD, MemberRole.GUEST):
            return False
        if role == MemberRole.TEEN:
            return action == ActionType.READ and is_owner # Teen can view their own allowance/spending
        return True

    # 5. Parents / Guardians manage household domain
    if role == MemberRole.PARENT_GUARDIAN:
        if action == ActionType.MANAGE_ROLES:
            return False
        return True

    # 6. Adults can read/write domestic domain
    if role in (MemberRole.ADULT, MemberRole.MEMBER):
        if action in (ActionType.READ, ActionType.CREATE):
            return True
        if action in (ActionType.UPDATE, ActionType.DELETE):
            return is_owner
        return False

    # 7. Teens can read/create standard domestic modules, read own data
    if role == MemberRole.TEEN:
        if resource in (ResourceType.MESSAGE, ResourceType.EVENT, ResourceType.TASK,
                        ResourceType.GALLERY, ResourceType.EDUCATION, ResourceType.TRIP,
                        ResourceType.PET, ResourceType.MEMORY, ResourceType.SAFETY):
            if action in (ActionType.READ, ActionType.CREATE):
                return True
            if action == ActionType.UPDATE and is_owner:
                return True
        return False

    # 8. Children have read-only access + status update on their assigned items
    if role == MemberRole.CHILD:
        if resource in (ResourceType.TASK, ResourceType.EVENT, ResourceType.MESSAGE,
                        ResourceType.GALLERY, ResourceType.EDUCATION, ResourceType.PET,
                        ResourceType.MEMORY):
            if action == ActionType.READ:
                return True
            if resource in (ResourceType.TASK, ResourceType.MESSAGE) and action in (ActionType.UPDATE, ActionType.CREATE):
                return True
        return False

    # 9. Guests
    if role == MemberRole.GUEST:
        return action == ActionType.READ and resource in (ResourceType.EVENT, ResourceType.HOUSEHOLD, ResourceType.GALLERY)

    return False

