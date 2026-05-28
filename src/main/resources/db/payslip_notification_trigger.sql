-- =============================================================================
-- ERP System — Payslip Salary Credit Notification Trigger
-- =============================================================================
-- Trigger : trg_payslip_paid_notification
-- Timing  : AFTER INSERT OR UPDATE OF payment_status ON payslips
-- Fires   : When payment_status transitions to 'PAID' (uppercase)
--
-- Message format (exam specification):
--   "Dear FIRSTNAME Your salary of MONTH/YEAR from INSTITUTION AMOUNT
--    has been credited to your EMPLOYEE_ID account Successfully."
--
-- HOW TO RUN (once, after the app has started at least once):
--   psql -U postgres -d erp_db -f payslip_notification_trigger.sql
--
-- VERIFY:
--   SELECT trigger_name, event_manipulation, action_timing, event_object_table
--   FROM   information_schema.triggers
--   WHERE  event_object_table = 'payslips';
--
--   SELECT id, receiver_name, message, sent_at FROM messages ORDER BY sent_at DESC;
-- =============================================================================

-- ── Step 1: Fix messages table — drop FK constraints and make sender_id nullable
-- =============================================================================

-- Drop any FK on messages that references employees (created by old Hibernate mappings)
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT tc.constraint_name
        FROM   information_schema.table_constraints tc
        JOIN   information_schema.referential_constraints rc
               ON tc.constraint_name = rc.constraint_name
        JOIN   information_schema.constraint_column_usage ccu
               ON rc.unique_constraint_name = ccu.constraint_name
        WHERE  tc.table_name      = 'messages'
          AND  tc.constraint_type = 'FOREIGN KEY'
          AND  ccu.table_name     = 'employees'
    LOOP
        EXECUTE 'ALTER TABLE messages DROP CONSTRAINT IF EXISTS ' || quote_ident(r.constraint_name);
        RAISE NOTICE 'Dropped FK constraint: %', r.constraint_name;
    END LOOP;
END;
$$;

-- Make sender_id nullable — system-generated messages have no employee sender.
-- This runs as a standalone DDL statement so it cannot be swallowed by an exception handler.
ALTER TABLE messages ALTER COLUMN sender_id DROP NOT NULL;

-- ── Step 2: Helper — convert "2025-05" → "May 2025" ──────────────────────────
DROP FUNCTION IF EXISTS erp_month_name(TEXT);

CREATE OR REPLACE FUNCTION erp_month_name(p_month TEXT)
RETURNS TEXT
LANGUAGE plpgsql
IMMUTABLE
AS $$
DECLARE
    v_year  INT;
    v_month INT;
BEGIN
    v_year  := SPLIT_PART(p_month, '-', 1)::INT;
    v_month := SPLIT_PART(p_month, '-', 2)::INT;
    RETURN CASE v_month
        WHEN  1 THEN 'January'    WHEN  2 THEN 'February'
        WHEN  3 THEN 'March'      WHEN  4 THEN 'April'
        WHEN  5 THEN 'May'        WHEN  6 THEN 'June'
        WHEN  7 THEN 'July'       WHEN  8 THEN 'August'
        WHEN  9 THEN 'September'  WHEN 10 THEN 'October'
        WHEN 11 THEN 'November'   WHEN 12 THEN 'December'
        ELSE p_month
    END || ' ' || v_year::TEXT;
EXCEPTION
    WHEN OTHERS THEN RETURN p_month;
END;
$$;

-- ── Step 3: Trigger function ──────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION fn_payslip_paid_notification()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_employee_id     BIGINT;
    v_first_name      TEXT;
    v_last_name       TEXT;
    v_formatted_month TEXT;
    v_amount_str      TEXT;
    v_message_text    TEXT;
    v_subject         TEXT;
    v_institution     TEXT := 'ERP Payroll System';
BEGIN
    -- ── Guard: only fire when status transitions TO 'PAID' ───────────────────
    IF TG_OP = 'UPDATE' THEN
        IF OLD.payment_status = 'PAID' OR NEW.payment_status <> 'PAID' THEN
            RETURN NEW;
        END IF;
    ELSIF TG_OP = 'INSERT' THEN
        IF NEW.payment_status <> 'PAID' THEN
            RETURN NEW;
        END IF;
    END IF;

    -- ── Fetch employee name ───────────────────────────────────────────────────
    SELECT e.id, e.first_name, e.last_name
    INTO   v_employee_id, v_first_name, v_last_name
    FROM   employees e
    WHERE  e.id = NEW.employee_id;

    IF NOT FOUND THEN
        RAISE WARNING '[trigger] Employee id=% not found for payslip id=%',
                      NEW.employee_id, NEW.id;
        RETURN NEW;
    END IF;

    -- ── Build message ─────────────────────────────────────────────────────────
    v_formatted_month := erp_month_name(NEW.payroll_month);
    v_amount_str      := COALESCE(NEW.currency, 'RWF') || ' ' ||
                         TO_CHAR(NEW.net_salary, 'FM999,999,999,990.00');

    v_subject := 'Salary Credit Notification – ' || v_formatted_month;

    -- Exact exam format:
    -- "Dear FIRSTNAME Your salary of MONTH/YEAR from INSTITUTION AMOUNT
    --  has been credited to your EMPLOYEE_ID account Successfully."
    v_message_text := FORMAT(
        'Dear %s Your salary of %s from %s %s has been credited to your %s account Successfully.',
        v_first_name,
        v_formatted_month,
        v_institution,
        v_amount_str,
        NEW.employee_id::TEXT
    );

    -- ── Insert into messages ──────────────────────────────────────────────────
    -- sender_id is NULL — this is a system-generated message, not from an employee
    INSERT INTO messages (
        sender_id,
        sender_name,
        receiver_id,
        receiver_name,
        subject,
        message,
        message_type,
        priority,
        is_read,
        is_archived,
        related_entity_type,
        related_entity_id,
        sent_at,
        created_at,
        updated_at,
        is_deleted
    ) VALUES (
        NULL,                                         -- system sender (no employee FK)
        v_institution,
        v_employee_id,
        v_first_name || ' ' || v_last_name,
        v_subject,
        v_message_text,
        'Notification',
        'High',
        FALSE,
        FALSE,
        'Payslip',
        NEW.id,
        NOW(),
        NOW(),
        NOW(),
        FALSE
    );

    RAISE NOTICE '[trigger] Message inserted for employee=% payslip=% month=%',
                 v_employee_id, NEW.id, NEW.payroll_month;

    RETURN NEW;

EXCEPTION
    WHEN OTHERS THEN
        RAISE WARNING '[trigger] Error for payslip %: %', NEW.id, SQLERRM;
        RETURN NEW;
END;
$$;

-- ── Step 4: Drop old trigger and install fresh ────────────────────────────────
DROP TRIGGER IF EXISTS trg_payslip_paid_notification ON payslips;

CREATE TRIGGER trg_payslip_paid_notification
    AFTER INSERT OR UPDATE OF payment_status
    ON payslips
    FOR EACH ROW
    EXECUTE FUNCTION fn_payslip_paid_notification();

-- ── Step 5: Confirm ───────────────────────────────────────────────────────────
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.triggers
        WHERE  trigger_name        = 'trg_payslip_paid_notification'
          AND  event_object_table  = 'payslips'
    ) THEN
        RAISE NOTICE '✓ Trigger trg_payslip_paid_notification installed on payslips';
    ELSE
        RAISE WARNING '✗ Trigger NOT found — check errors above';
    END IF;
END;
$$;

-- ── Step 6: Backfill — fire trigger for existing PAID payslips with no message
-- This re-triggers the notification for rows that were PAID before the trigger existed.
UPDATE payslips
SET    payment_status = 'PAID'
WHERE  payment_status = 'PAID'
  AND  NOT EXISTS (
           SELECT 1 FROM messages m
           WHERE  m.related_entity_type = 'Payslip'
             AND  m.related_entity_id   = payslips.id
       );
