-- =============================================================================
-- ERP System — Payslip Notification Trigger
-- =============================================================================
-- Trigger: trg_payslip_paid_notification
-- Fires   : AFTER INSERT OR UPDATE on the payslips table
-- Purpose : When a payslip's payment_status is set to 'Paid', automatically:
--             1. Insert a salary-credit notification into the messages table.
--             2. Update the payslip's payment_date to NOW() if not already set.
--
-- Message format:
--   "Dear FIRSTNAME, your salary of MONTH/YEAR from INSTITUTION
--    amount AMOUNT has been credited successfully."
--
-- Prerequisites:
--   • Tables: payslips, employees, messages  (created by Hibernate DDL)
--   • Run this script AFTER the application has started at least once
--     so that Hibernate has created all tables.
--
-- Usage:
--   psql -U postgres -d erp_db -f payslip_notification_trigger.sql
-- =============================================================================

-- ── Helper: month name from YYYY-MM string ────────────────────────────────────
CREATE OR REPLACE FUNCTION erp_month_name(payroll_month TEXT)
RETURNS TEXT
LANGUAGE plpgsql
IMMUTABLE
AS $$
DECLARE
    month_num  INT;
    year_num   INT;
    month_name TEXT;
BEGIN
    year_num  := SPLIT_PART(payroll_month, '-', 1)::INT;
    month_num := SPLIT_PART(payroll_month, '-', 2)::INT;

    month_name := CASE month_num
        WHEN  1 THEN 'January'
        WHEN  2 THEN 'February'
        WHEN  3 THEN 'March'
        WHEN  4 THEN 'April'
        WHEN  5 THEN 'May'
        WHEN  6 THEN 'June'
        WHEN  7 THEN 'July'
        WHEN  8 THEN 'August'
        WHEN  9 THEN 'September'
        WHEN 10 THEN 'October'
        WHEN 11 THEN 'November'
        WHEN 12 THEN 'December'
        ELSE payroll_month
    END;

    RETURN month_name || ' ' || year_num;
END;
$$;

-- ── Trigger function ──────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION fn_payslip_paid_notification()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_employee_id    BIGINT;
    v_first_name     TEXT;
    v_last_name      TEXT;
    v_email          TEXT;
    v_payroll_month  TEXT;
    v_net_salary     NUMERIC(10,2);
    v_currency       TEXT;
    v_formatted_month TEXT;
    v_amount_str     TEXT;
    v_message_text   TEXT;
    v_subject        TEXT;
    v_institution    TEXT := 'ERP Payroll System';
BEGIN
    -- ── Only act when payment_status transitions to 'Paid' ───────────────────
    -- For INSERT: fire if new row is already Paid
    -- For UPDATE: fire only when status changes from non-Paid to Paid
    IF TG_OP = 'UPDATE' THEN
        IF OLD.payment_status = 'Paid' OR NEW.payment_status <> 'Paid' THEN
            RETURN NEW;  -- no change needed
        END IF;
    ELSIF TG_OP = 'INSERT' THEN
        IF NEW.payment_status <> 'Paid' THEN
            RETURN NEW;  -- not paid yet
        END IF;
    END IF;

    -- ── Set payment_date if not already set ──────────────────────────────────
    IF NEW.payment_date IS NULL THEN
        NEW.payment_date := CURRENT_DATE;
    END IF;

    -- ── Fetch employee details ────────────────────────────────────────────────
    SELECT e.id, e.first_name, e.last_name, e.email
    INTO   v_employee_id, v_first_name, v_last_name, v_email
    FROM   employees e
    WHERE  e.id = NEW.employee_id;

    IF NOT FOUND THEN
        RAISE WARNING 'payslip_notification: employee % not found for payslip %',
                      NEW.employee_id, NEW.id;
        RETURN NEW;
    END IF;

    -- ── Build message ─────────────────────────────────────────────────────────
    v_payroll_month   := NEW.payroll_month;                        -- YYYY-MM
    v_net_salary      := NEW.net_salary;
    v_currency        := COALESCE(NEW.currency, 'USD');
    v_formatted_month := erp_month_name(v_payroll_month);          -- e.g. "May 2025"
    v_amount_str      := v_currency || ' ' ||
                         TO_CHAR(v_net_salary, 'FM999,999,990.00'); -- e.g. "USD 4,500.00"

    v_subject      := 'Salary Credit – ' || v_formatted_month;
    v_message_text := FORMAT(
        'Dear %s Your salary of %s from %s %s has been credited to your %s account Successfully.',
        v_first_name,
        v_formatted_month,
        v_institution,
        v_amount_str,
        NEW.employee_id::TEXT
    );

    -- ── If the application already created a notification for this payslip, skip insertion
    IF EXISTS (
        SELECT 1 FROM messages m
        WHERE m.related_entity_type = 'Payslip' AND m.related_entity_id = NEW.id
    ) THEN
        RAISE NOTICE 'payslip_notification: message already exists for payslip % — skipping DB insert', NEW.id;
        RETURN NEW;
    END IF;

    -- ── Insert notification into messages table ───────────────────────────────
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
        0,                                          -- system sender (id=0)
        v_institution,                              -- sender_name
        v_employee_id,                              -- receiver_id
        v_first_name || ' ' || v_last_name,         -- receiver_name
        v_subject,                                  -- subject
        v_message_text,                             -- message body
        'Notification',                             -- message_type
        'High',                                     -- priority
        FALSE,                                      -- is_read
        FALSE,                                      -- is_archived
        'Payslip',                                  -- related_entity_type
        NEW.id,                                     -- related_entity_id
        NOW(),                                      -- sent_at
        NOW(),                                      -- created_at
        NOW(),                                      -- updated_at
        FALSE                                       -- is_deleted
    );

    RAISE NOTICE 'Salary credit notification inserted for employee % (payslip %)',
                 v_employee_id, NEW.id;

    RETURN NEW;

EXCEPTION
    WHEN OTHERS THEN
        -- Log the error but never block the payslip update
        RAISE WARNING 'payslip_notification trigger error for payslip %: %',
                      NEW.id, SQLERRM;
        RETURN NEW;
END;
$$;

-- ── Drop old trigger if it exists, then create fresh ─────────────────────────
DROP TRIGGER IF EXISTS trg_payslip_paid_notification ON payslips;

CREATE TRIGGER trg_payslip_paid_notification
    AFTER INSERT OR UPDATE OF payment_status
    ON payslips
    FOR EACH ROW
    EXECUTE FUNCTION fn_payslip_paid_notification();

-- ── Verification ──────────────────────────────────────────────────────────────
-- Run these queries to confirm the trigger is installed:
--
--   SELECT trigger_name, event_manipulation, action_timing
--   FROM   information_schema.triggers
--   WHERE  event_object_table = 'payslips';
--
--   SELECT routine_name FROM information_schema.routines
--   WHERE  routine_name IN ('fn_payslip_paid_notification', 'erp_month_name');
-- =============================================================================
